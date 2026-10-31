# Grafana + VictoriaLogs + VictoriaMetrics в minikube

Grafana, подключённая к Victoria Logs и Victoria Metrics. Работает в namespace
`cookbook`. Для логов используется официальный плагин
`victoriametrics-logs-datasource`, для метрик — стандартный datasource
`Prometheus`.

## Состав
- `00-pvc.yaml` — `PersistentVolumeClaim grafana-pvc` (1Gi) — данные Grafana (дашборды, установленный плагин);
- `01-datasource-configmap.yaml` — `ConfigMap grafana-datasources` с provisioning datasources на `http://victoria-logs:9428` и `http://vm-single-victoria-metrics-single-server.monitoring.svc:8428`;
- `02-deployment.yaml` — `Deployment grafana` (образ `grafana/grafana:12.0.2`, `GF_INSTALL_PLUGINS` = URL GitHub-релиза плагина, `fsGroup: 472`, логин/пароль из `Secret grafana-secret`);
- `03-service.yaml` — `Service grafana` (NodePort `30300`).

`kustomization.yaml` собирает манифесты и генерирует `grafana-secret` из локального `grafana-secret.env` (не хранится в git).

> **Важно:** Grafana открывается только при включённом VPN — без него каталог
> плагинов grafana.com недоступен (HTTP 451), и плагин не установится. Поэтому
> плагин ставится из GitHub-релиза VictoriaMetrics напрямую.

## Запуск

Namespace `cookbook` должен существовать:

```bash
kubectl apply -f k8s/spring-boot/00-namespace.yaml
```

Создайте `k8s/grafana/grafana-secret.env`:

```env
admin-user=admin
admin-password=admin
```

```bash
kubectl apply -k k8s/grafana/
kubectl get pods -n cookbook -l app=grafana

# дождаться установки плагина (Grafana ставит его при старте, нужен интернет)
kubectl logs -n cookbook deploy/grafana | grep -i plugin
```

## Доступ
```bash
kubectl port-forward svc/grafana 3000:3000 -n cookbook
# UI: http://localhost:3000  (admin / admin)
# либо: minikube service grafana -n cookbook
```

Datasource **VictoriaLogs** подключается автоматически (provisioning), уже выставлен
по умолчанию. Datasource **VictoriaMetrics** типа `Prometheus` подключается
к `http://vm-single-victoria-metrics-single-server.monitoring.svc:8428`.

## Дашборд

Дашборд `SLI / SLO / SLA` лежит в `k8s/grafana/dashboards/SLI_SLO_SLA.json`.
Импорт: **Dashboards → New → Import → Upload JSON**, затем выбрать datasource
`VictoriaMetrics`. Файл экспортирован с «Export for sharing externally»,
поэтому datasource подставляется при импорте.

## Проверка
Grafana → **Explore** → datasource `VictoriaLogs` → LogsQL-запрос, например:
```
_time:1h level:ERROR
_time:1h | stats by (level) count()
```

Метрики: **Explore** → datasource `VictoriaMetrics` → PromQL, например:
```promql
up{namespace="cookbook"}
cookbook_probe_runs_total
```

## Траблшутинг
- `Error: ✗ 451` — каталог grafana.com недоступен (нет VPN); плагин должен тянуться
  из GitHub-релиза (см. `02-deployment.yaml`).
- Плагин не установился — нужен исходящий интернет из пода (или VPN):
  `kubectl logs -n cookbook deploy/grafana | grep -iE 'plugin|download|451'`.
- Datasource «unknown type» — плагин ещё не доустановился, подождать/перезапустить под.
- Пусто в панели — проверить диапазон времени и имена полей (как в vmui).

## Удаление
```bash
kubectl delete -k k8s/grafana/
```
