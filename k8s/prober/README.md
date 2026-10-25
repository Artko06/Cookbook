# Пробер Cookbook

Минимальный пробер для проверки публичного API приложения.
Работает в namespace `cookbook`, метрики скрейпятся VictoriaMetrics по аннотациям.

## Состав

| Файл | Назначение |
|------|-----------|
| `prober.py` | цикл проверки, `/metrics`, `/healthz` |
| `01-deployment.yaml` | `Deployment prober`, 1 pod, порт `9090` |
| `kustomization.yaml` | сборка манифестов и ConfigMap из `prober.py` |

Порт `9090` — только для метрик пробера. Приложение доступно отдельно на `app:8080`.

## Проверка

```bash
kubectl apply -k k8s/prober/
kubectl get pods -n cookbook -l app=prober
kubectl logs -n cookbook deploy/prober
```

Пробер раз в 15 секунд выполняет:

```text
GET http://app:8080/api/v1/recipes?size=5
```

Успех — HTTP `2xx` и валидный JSON.

## Метрики

```bash
kubectl port-forward deploy/prober 9090:9090 -n cookbook
curl http://localhost:9090/metrics
```

| Метрика | Тип | Назначение |
|---------|-----|-----------|
| `cookbook_probe_runs_total{result="success\|failure"}` | counter | число успешных и неуспешных прогонов |
| `cookbook_probe_duration_seconds` | gauge | длительность последнего прогона |
| `cookbook_probe_up` | gauge | жив ли цикл пробера |

## Проверка в VictoriaMetrics

```bash
kubectl port-forward svc/vm-single-victoria-metrics-single-server -n monitoring 8428:8428
```

```text
http://localhost:8428/targets
http://localhost:8428/vmui/
```

Запросы:

```promql
up{namespace="cookbook",app="prober"}
cookbook_probe_runs_total
cookbook_probe_duration_seconds
cookbook_probe_up
```

SLI:

```promql
sum(increase(cookbook_probe_runs_total{result="success"}[<window>]))
/
sum(increase(cookbook_probe_runs_total[<window>]))
```

## Настройки

| Переменная | По умолчанию | Назначение |
|-----------|--------------|-----------|
| `PROBE_URL` | `http://app:8080/api/v1/recipes?size=5` | проверяемый URL |
| `PROBE_INTERVAL_SECONDS` | `15` | интервал между прогонами |
| `PROBE_TIMEOUT_SECONDS` | `5` | таймаут запроса |
| `PROBE_PORT` | `9090` | порт `/metrics` и `/healthz` |
| `LOG_LEVEL` | `INFO` | уровень логирования |

## Удаление

```bash
kubectl delete -k k8s/prober/
```
