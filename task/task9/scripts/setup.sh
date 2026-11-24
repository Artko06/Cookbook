#!/usr/bin/env bash
# Поднимает локальное окружение для task9:
#   minikube -> Envoy Gateway -> PostgreSQL -> Gateway/ConfigMap.
# Приложения v1/v2 и HTTPRoute с весами 90/10 применяются отдельно.
# k8s/spring-boot — общий слой приложения, зависимости от task8 нет.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
MANIFEST_DIR="$ROOT_DIR/k8s/gateway-weights"

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

[[ "$(kubectl config current-context 2>/dev/null)" == "minikube" ]] \
    || die "текущий Kubernetes-контекст не minikube — переключитесь на minikube"
[[ -f "$ROOT_DIR/k8s/spring-boot/db-secret.env" ]] \
    || die "не найден k8s/spring-boot/db-secret.env — создайте его по примеру k8s/spring-boot/README.md"
[[ -d "$MANIFEST_DIR" ]] || die "не найдена папка манифестов $MANIFEST_DIR"

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
    -f "$MANIFEST_DIR/00-namespace.yaml"

SECRET_FILE="$ROOT_DIR/k8s/spring-boot/db-secret.env"
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
# ConfigMap тоже namespace-scoped: копируем его для task9 и меняем адрес БД
# на полное DNS-имя, потому что `db` живёт в namespace $BASE_NS.
sed -e "s|^  namespace: cookbook$|  namespace: $NS|" \
    -e "s|jdbc:postgresql://db:|jdbc:postgresql://db.$BASE_NS.svc.cluster.local:|" \
    "$ROOT_DIR/k8s/spring-boot/04-app-configmap.yaml" \
    | kubectl apply -f -

kubectl apply -f "$MANIFEST_DIR/03-gateway.yaml"
kubectl wait --timeout=3m -n "$NS" "gateway/$GATEWAY_NAME" \
    --for=condition=Accepted

# --- 6. Приложения и HTTPRoute task9 ---------------------------------------
# В namespace могли остаться HTTPRoute и rolling Deployment от task8.
# Удаляем их, иначе несколько маршрутов могут одновременно влиять на трафик,
# а старый Deployment cookbook — попасть в selector cookbook-v2.
log "Убираем старые HTTPRoute и rolling Deployment cookbook"
if kubectl get crd httproutes.gateway.networking.k8s.io >/dev/null 2>&1; then
    kubectl delete httproute --all -n "$NS" --ignore-not-found
fi
kubectl delete deployment cookbook -n "$NS" --ignore-not-found
kubectl delete service cookbook -n "$NS" --ignore-not-found

log "Применяем приложения v1/v2 и HTTPRoute 90/10"
kubectl apply -k "$MANIFEST_DIR"

kubectl rollout status deploy/cookbook-v1 -n "$NS" --timeout=300s
kubectl rollout status deploy/cookbook-v2 -n "$NS" --timeout=300s

kubectl get deployment,service,gateway,httproute -n "$NS"
kubectl get pods -n "$NS" -l app=cookbook -o wide

cat <<EOF

Готово.
  Приложения: namespace $NS
  PostgreSQL, app-config, db-secret: namespace $BASE_NS
  HTTPRoute cookbook: 90% на cookbook-v1, 10% на cookbook-v2

Дальше:
  # трафик через Envoy-прокси
  ./task/task9/scripts/traffic.sh 200

  # результат также сохранится в task/task9/results/
EOF
