#!/usr/bin/env bash
# Сбрасывает приложения и HTTPRoute между стратегиями.
# НЕ трогает PostgreSQL, Gateway, ConfigMap и мониторинг.
set -euo pipefail

NS="${NS:-cookbook-deploy}"

echo "Удаляю HTTPRoute и приложения в namespace $NS ..."

if kubectl get crd httproutes.gateway.networking.k8s.io >/dev/null 2>&1; then
    kubectl delete httproute --all -n "$NS" --ignore-not-found
else
    echo "CRD HTTPRoute не установлены — пропускаю удаление маршрутов."
fi

kubectl delete deployment cookbook cookbook-v1 cookbook-v2 \
    -n "$NS" --ignore-not-found
kubectl delete service cookbook cookbook-v1 cookbook-v2 \
    -n "$NS" --ignore-not-found

echo "Готово. БД, Gateway и мониторинг на месте."
echo "Можно применять следующую стратегию."
