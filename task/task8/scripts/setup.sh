#!/usr/bin/env bash
# Поднимает локальное окружение для стратегий деплоя Cookbook:
#   minikube -> Envoy Gateway -> PostgreSQL -> Gateway/ConfigMap.
# Приложение и конкретные стратегии применяются отдельно.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

BASE_NS="${BASE_NS:-cookbook}"
NS="${NS:-cookbook-deploy}"
GATEWAY_NAME="${GATEWAY_NAME:-eg}"
EG_NAMESPACE="${EG_NAMESPACE:-envoy-gateway-system}"
ENVOY_GATEWAY_VERSION="${ENVOY_GATEWAY_VERSION:-v1.10.0}"
APP_IMAGE="${APP_IMAGE:-cookbook-app:latest}"

log() { printf '\n\033[1;32m==> %s\033[0m\n' "$*"; }
die() { printf '\033[1;31mОШИБКА: %s\033[0m\n' "$*" >&2; exit 1; }

for bin in kubectl minikube docker; do
    command -v "$bin" >/dev/null 2>&1 || die "не найден $bin — установите его и повторите"
done

# --- 1. minikube ------------------------------------------------------------
if minikube status >/dev/null 2>&1; then
    log "minikube уже запущен"
else
    log "Запускаем minikube (docker-драйвер)"
    minikube start --driver=docker \
        --cpus="${MINIKUBE_CPUS:-4}" \
        --memory="${MINIKUBE_MEMORY:-8192}" \
        --disk-size="${MINIKUBE_DISK:-40g}"
fi

# --- 2. Образ приложения ----------------------------------------------------
log "Собираем образ ${APP_IMAGE} в Docker daemon minikube"
eval "$(minikube docker-env)"
docker build -t "$APP_IMAGE" "$ROOT_DIR"

# --- 3. Envoy Gateway -------------------------------------------------------
log "Проверяем Envoy Gateway ${ENVOY_GATEWAY_VERSION}"
if ! kubectl get gatewayclass eg >/dev/null 2>&1 \
    || ! kubectl get deployment envoy-gateway -n "$EG_NAMESPACE" >/dev/null 2>&1; then
    kubectl apply --server-side -f \
        "https://github.com/envoyproxy/gateway/releases/download/${ENVOY_GATEWAY_VERSION}/install.yaml"
fi
kubectl wait --timeout=5m -n "$EG_NAMESPACE" deployment/envoy-gateway \
    --for=condition=Available

# --- 4. Namespace, БД и секрет ---------------------------------------------
log "Применяем namespace и секрет БД"
kubectl apply \
    -f "$ROOT_DIR/k8s/spring-boot/00-namespace.yaml" \
    -f "$ROOT_DIR/k8s/deploy-strategies/00-namespace.yaml"

SECRET_FILE="$ROOT_DIR/k8s/spring-boot/db-secret.env"
if [[ ! -f "$SECRET_FILE" ]]; then
    die "не найден $SECRET_FILE — создайте его по примеру k8s/spring-boot/README.md"
fi
# Secret нельзя использовать из другого namespace: создаём копии в обоих.
for ns in "$BASE_NS" "$NS"; do
    kubectl create secret generic db-secret -n "$ns" \
        --from-env-file="$SECRET_FILE" \
        --dry-run=client -o yaml | kubectl apply -f -
done

log "Применяем PostgreSQL"
kubectl apply \
    -f "$ROOT_DIR/k8s/spring-boot/01-db-pvc.yaml" \
    -f "$ROOT_DIR/k8s/spring-boot/02-db-deployment.yaml" \
    -f "$ROOT_DIR/k8s/spring-boot/03-db-service.yaml"
kubectl rollout status deploy/db -n "$BASE_NS" --timeout=180s

# --- 5. ConfigMap и Gateway -------------------------------------------------
log "Применяем ConfigMap и Gateway"
kubectl apply -f "$ROOT_DIR/k8s/spring-boot/04-app-configmap.yaml"
# ConfigMap тоже namespace-scoped: копируем его для стенда и меняем адрес БД
# на полное DNS-имя, потому что `db` живёт в namespace $BASE_NS.
sed -e "s|^  namespace: cookbook$|  namespace: $NS|" \
    -e "s|jdbc:postgresql://db:|jdbc:postgresql://db.$BASE_NS.svc.cluster.local:|" \
    "$ROOT_DIR/k8s/spring-boot/04-app-configmap.yaml" \
    | kubectl apply -f -

kubectl apply -f "$ROOT_DIR/k8s/deploy-strategies/01-gateway.yaml"
kubectl wait --timeout=3m -n "$NS" "gateway/$GATEWAY_NAME" \
    --for=condition=Accepted

cat <<EOF

Готово.
  Стратегии, Gateway и HTTPRoute: namespace $NS
  PostgreSQL, app-config, db-secret, мониторинг: namespace $BASE_NS

Дальше:
  # трафик через Envoy-прокси
  ./task/task8/scripts/traffic.sh

  # пример: rolling
  kubectl apply -f k8s/deploy-strategies/strategies/rolling/01-app-v1.yaml
  kubectl apply -f k8s/deploy-strategies/strategies/rolling/02-app-v2.yaml

  # перед следующей стратегией
  ./task/task8/scripts/reset.sh

Полный сценарий: k8s/deploy-strategies/README.md
EOF
