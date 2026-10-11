# Victoria Logs (single-node) в minikube

Манифесты для развёртывания Victoria Logs в namespace `cookbook` 

## Состав
- `00-pvc.yaml` — `PersistentVolumeClaim victoria-logs-pvc` (2Gi) — хранение логов;
- `01-deployment.yaml` — `Deployment victoria-logs` (`victoriametrics/victoria-logs:v1.53.0`, `-storageDataPath=/vlogs`, порт `9428`);
- `02-service.yaml` — `Service victoria-logs` (`ClusterIP:9428`).

## Запуск
```bash
# при необходимости сначала namespace приложения
kubectl apply -f k8s/spring-boot/00-namespace.yaml

# Victoria Logs
kubectl apply -f k8s/victoria-logs/

# Проверка
kubectl get pods,pvc,svc -n cookbook
```

## Доступ к vmui
```bash
kubectl port-forward svc/victoria-logs 9428:9428 -n cookbook
# UI: http://localhost:9428/vmui/  (в новых версиях — /select/vmui/)
```

## Внутренний адрес (для шиппера)
```
http://victoria-logs:9428
```
Эндпоинты приёма логов:
- Elasticsearch bulk: `/insert/elasticsearch/_bulk`
- JSON lines: `/insert/jsonline`
- Loki: `/insert/loki/api/v1/push`

## Удаление
```bash
kubectl delete -f k8s/victoria-logs/
```
