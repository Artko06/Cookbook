# SLI/SLO queries

Окна:

- SLI/SLO — `window` (по умолчанию `1h`);
- SLA — SLI за `5m`, окно `1h`;
- SLA target — `99%` времени выполнения SLO.

| SLI | SLO 1h |
|---|---:|
| `app_uptime` | 0.99 |
| `http_availability` | 0.95 |
| `recipe_operations` | 0.95 |
| `browse_availability` | 0.95 |
| `db_uptime` | 0.99 |

## SLI

### app_uptime

```promql
sum(sum_over_time(up{job="kubernetes-pods",namespace="cookbook",app="app"}[$window]))
/
sum(count_over_time(up{job="kubernetes-pods",namespace="cookbook",app="app"}[$window]))
```

### http_availability

```promql
sum(increase(http_server_requests_seconds_count{
  application="cookbook",
  uri!~"/actuator.*",
  status!~"5.."
}[$window]))
/
sum(increase(http_server_requests_seconds_count{
  application="cookbook",
  uri!~"/actuator.*"
}[$window]))
```

### recipe_operations

```promql
sum(increase(http_server_requests_seconds_count{
  application="cookbook",
  uri=~"/recipes.*|/api/v1/recipes.*",
  status!~"5.."
}[$window]))
/
sum(increase(http_server_requests_seconds_count{
  application="cookbook",
  uri=~"/recipes.*|/api/v1/recipes.*"
}[$window]))
```

### browse_availability

```promql
sum(increase(cookbook_probe_runs_total{result="success"}[$window]))
/
sum(increase(cookbook_probe_runs_total[$window]))
```

### db_uptime

```promql
sum(sum_over_time(pg_up{namespace="cookbook"}[$window]))
/
sum(count_over_time(pg_up{namespace="cookbook"}[$window]))
```

## Линии SLO

| SLI | Запрос |
|---|---|
| `app_uptime` | `vector(0.99)` |
| `http_availability` | `vector(0.95)` |
| `recipe_operations` | `vector(0.95)` |
| `browse_availability` | `vector(0.95)` |
| `db_uptime` | `vector(0.99)` |

## SLA compliance

SLA для приложения: SLO `HTTP availability (5m) >= 95%` выполняется `>= 99%` времени за `1h`.

```promql
avg_over_time((
  (
    sum(increase(http_server_requests_seconds_count{
      application="cookbook",
      uri!~"/actuator.*",
      status!~"5.."
    }[5m]))
    /
    sum(increase(http_server_requests_seconds_count{
      application="cookbook",
      uri!~"/actuator.*"
    }[5m]))
  ) >= bool 0.95
)[1h:])
or vector(0)
```

Если Prometheus не принимает `[1h:]`, использовать `[1h:1m]`.

## SLA target

```promql
vector(0.99)
```
