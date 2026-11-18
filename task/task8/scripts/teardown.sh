#!/usr/bin/env bash
# Убирает ресурсы задания, не удаляя PostgreSQL и мониторинг.
# По умолчанию Envoy Gateway остаётся. Флаг --envoy удаляет и его.
set -euo pipefail

BASE_NS="${BASE_NS:-cookbook}"
NS="${NS:-cookbook-deploy}"
GATEWAY_NAME="${GATEWAY_NAME:-eg}"
EG_NAMESPACE="${EG_NAMESPACE:-envoy-gateway-system}"
ENVOY_GATEWAY_VERSION="${ENVOY_GATEWAY_VERSION:-v1.10.0}"
REMOVE_ENVOY=0

for arg in "$@"; do
    case "$arg" in
        --envoy) REMOVE_ENVOY=1 ;;
        -h|--help)
            echo "Использование: $0 [--envoy]"
            echo "  --envoy  удалить также Envoy Gateway"
            exit 0
            ;;
        *)
            echo "Неизвестный аргумент: $arg" >&2
            exit 2
            ;;
    esac
done

echo "Удаляю приложения и маршруты в namespace $NS ..."

if kubectl get crd httproutes.gateway.networking.k8s.io >/dev/null 2>&1; then
    kubectl delete httproute --all -n "$NS" --ignore-not-found
    kubectl delete gateway "$GATEWAY_NAME" -n "$NS" --ignore-not-found
fi

kubectl delete deployment cookbook cookbook-v1 cookbook-v2 \
    -n "$NS" --ignore-not-found
kubectl delete service cookbook cookbook-v1 cookbook-v2 \
    -n "$NS" --ignore-not-found

if [[ "$NS" != "$BASE_NS" ]]; then
    echo "Удаляю namespace $NS ..."
    kubectl delete namespace "$NS" --ignore-not-found
fi

if [[ "$REMOVE_ENVOY" == "1" ]]; then
    echo "Удаляю Envoy Gateway ${ENVOY_GATEWAY_VERSION} ..."
    kubectl delete -f \
        "https://github.com/envoyproxy/gateway/releases/download/${ENVOY_GATEWAY_VERSION}/install.yaml" \
        --ignore-not-found >/dev/null 2>&1 || true
    kubectl delete namespace "$EG_NAMESPACE" --ignore-not-found >/dev/null 2>&1 || true
fi

echo "Готово. PostgreSQL, секрет БД и мониторинг не тронуты."
