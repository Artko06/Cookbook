# Task9: распределение трафика 90/10 через Gateway API

Самодостаточный набор манифестов для задания №9: две версии приложения
Cookbook, отдельные ClusterIP Service и HTTPRoute с весами 90/10.

| Файл | Содержимое |
|---|---|
| `00-namespace.yaml` | namespace `cookbook-deploy` |
| `01-app-v1.yaml` | Deployment `cookbook-v1` + Service `cookbook-v1` (ClusterIP) |
| `02-app-v2.yaml` | Deployment `cookbook-v2` + Service `cookbook-v2` (ClusterIP) |
| `03-gateway.yaml` | GatewayClass `eg` + Gateway `eg` |
| `04-httproute-90-10.yaml` | HTTPRoute `cookbook`: 90% на v1, 10% на v2 |

## Запуск

```bash
./task/task9/scripts/setup.sh
./task/task9/scripts/traffic.sh 200
```

`setup.sh` поднимает minikube, собирает образ,
устанавливает Envoy Gateway и применяет общий слой `k8s/spring-boot`
(PostgreSQL, `db-secret`, `app-config`), после чего разворачивает манифесты
task9 из `k8s/gateway-weights/`.

## Проверка

```bash
kubectl get deployment,service,gateway,httproute -n cookbook-deploy
kubectl get httproute cookbook -n cookbook-deploy -o yaml
```

В `backendRefs` должны быть веса `90` и `10`. Результаты запросов сохраняются
в `task/task9/results/`.

## Удаление

```bash
./task/task9/scripts/teardown.sh
```
