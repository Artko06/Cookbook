#!/usr/bin/env bash
# Удаляет HTTPRoute и приложения, не трогая PostgreSQL, Gateway
# и общую инфраструктуру.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
NS="${NS:-cookbook-deploy}"

[[ "$(kubectl config current-context 2>/dev/null)" == "minikube" ]] \
    || { printf 'ОШИБКА: текущий контекст не minikube\n' >&2; exit 1; }

echo "Удаляю HTTPRoute и приложения task9 в namespace $NS ..."
if kubectl get crd httproutes.gateway.networking.k8s.io >/dev/null 2>&1; then
    kubectl delete -f "$ROOT_DIR/k8s/gateway-weights/04-httproute-90-10.yaml" --ignore-not-found
fi
kubectl delete \
    -f "$ROOT_DIR/k8s/gateway-weights/01-app-v1.yaml" \
    -f "$ROOT_DIR/k8s/gateway-weights/02-app-v2.yaml" \
    --ignore-not-found

echo "Готово. PostgreSQL, Gateway eg и namespace $NS оставлены."
