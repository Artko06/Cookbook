# Vector (log shipper) в minikube

DaemonSet-шиппер: читает контейнерные логи ноды, парсит JSON-логи приложения и
отправляет их в Victoria Logs. Работает в namespace `cookbook`.

## Состав
- `00-serviceaccount.yaml` — `ServiceAccount vector`;
- `01-rbac.yaml` — `ClusterRole`/`ClusterRoleBinding` (pods/namespaces/nodes: get,list,watch);
- `02-configmap.yaml` — конфиг Vector (`kubernetes_logs` → парсинг JSON → фильтр `cookbook` → sink в VL);
- `03-daemonset.yaml` — `DaemonSet vector` (hostPath `/var/log`, `/var/lib`; образ `timberio/vector:0.58.0-debian`).

## Запуск
```bash
kubectl apply -f k8s/vector/
kubectl get pods -n cookbook
kubectl logs -n cookbook daemonset/vector
```

## Проверка (после запуска Victoria Logs)
```bash
kubectl port-forward svc/victoria-logs 9428:9428 -n cookbook
# http://localhost:9428/select/vmui/
```

## Поля
- `message` — текст сообщения (`_msg` в Victoria Logs);
- `timestamp` — время из лога (используется как `_time`);
- `level`, `service.name` — из JSON-логов приложения;
- `kubernetes.*` — метаданные (namespace, pod, container), добавляет Vector.
- Шиппер оставляет только логи контейнера `app` из namespace `cookbook`.

## Траблшутинг
- В sink можно временно добавить `query: {debug: "1"}` — Victoria Logs залогирует входящие строки:
  `kubectl logs -n cookbook deploy/victoria-logs`.
- Если логи не появляются — проверить `kubectl logs -n cookbook daemonset/vector` на ошибки парсинга/sink.

## Удаление
```bash
kubectl delete -f k8s/vector/
```
