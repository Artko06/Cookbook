# Cookbook: стратегии деплоя в minikube

Локальный стенд к домашнему заданию №8: **minikube → Envoy Gateway (Gateway API) →
приложение Cookbook в нескольких репликах → PostgreSQL**.

Показываем все пять стратегий:

- **Rolling** — постепенная замена подов;
- **Blue/Green** — мгновенное переключение и откат;
- **Canary** — `90/10 → 50/50 → 0/100`;
- **A/B** — выбранная аудитория по заголовку;
- **Shadow** — зеркалирование копии трафика.

Трафик между версиями делит **Gateway API** (`HTTPRoute`), а не Ingress и не
Istio VirtualService.

---

## Что поднимается

```text
                    ┌──────────────────────────────┐
                    │        minikube              │
                    │                              │
  curl :8888 ──────►│  Envoy Gateway (Gateway API) │
                    │        Gateway `eg`          │
                    │             │                │
                    │        HTTPRoute `cookbook`  │
                    │        /          \          │
                    │  cookbook-v1    cookbook-v2  │
                    │  (3 реплики)    (3 реплики)  │
                    │        \          /          │
                    │         PostgreSQL `db`      │
                    │      (сессии вне пода)       │
                    └──────────────────────────────┘
```

- `cookbook-deploy` — Gateway, HTTPRoute, приложения `cookbook*`;
- `cookbook` — общая инфраструктура: PostgreSQL, `db-secret`, `app-config`,
  мониторинг и legacy `app`;
- приложение — `cookbook-app:latest` из этого репозитория;
- PostgreSQL — существующий `db` из `k8s/spring-boot`;
- мониторинг — существующие VictoriaMetrics и Grafana;
- GatewayClass `eg`, Gateway `eg`;
- доступ снаружи — через `kubectl port-forward` на Envoy-прокси.

---

## Требования

- Docker;
- `minikube`, `kubectl`;
- ~4 CPU и ~8 GB RAM для minikube;
- доступ в интернет для образов и Envoy Gateway;
- собранный образ `cookbook-app:latest`;
- `k8s/spring-boot/db-secret.env` — из него `setup.sh` создаёт `db-secret`
  в `cookbook` и `cookbook-deploy`.

Проверить инструменты:

```bash
minikube version
kubectl version --client
docker version
```

---

## Быстрый старт

### 1. Поднять окружение

```bash
./task/task8/scripts/setup.sh
```

Скрипт:

1. проверяет `kubectl`, `minikube`, `docker`;
2. запускает minikube, если он остановлен;
3. собирает `cookbook-app:latest` в Docker daemon minikube;
4. устанавливает Envoy Gateway, если его нет;
5. применяет namespace `cookbook` и `cookbook-deploy`, `db-secret` и PostgreSQL;
6. копирует `app-config` в `cookbook-deploy` с `DB_URL` на
   `db.cookbook.svc.cluster.local` и применяет Gateway `eg`;
7. ждёт, пока Gateway станет `Accepted`.

Если `k8s/spring-boot/db-secret.env` отсутствует, создайте его по примеру из
`k8s/spring-boot/README.md`:

```env
POSTGRES_DB=cookbook
POSTGRES_USER=cookbook
POSTGRES_PASSWORD=cookbook
```

### 2. Запустить трафик

В отдельном терминале:

```bash
./task/task8/scripts/traffic.sh
```

Скрипт сам находит Envoy-прокси для Gateway `eg` в namespace `cookbook-deploy`,
пробрасывает локальный порт `:8888` и печатает версию и имя пода:

```text
23:30:01 | 200 | v1.0.0  | cookbook-v1-7d9f8c6b5-abcde
23:30:02 | 200 | v2.0.0  | cookbook-v2-6c8d7b9f4-fghij
```

Остановить — `Ctrl-C`.

### 3. Открыть Grafana

```bash
kubectl port-forward -n cookbook svc/grafana 3000:3000
```

Открыть http://localhost:3000 и импортировать:

```text
k8s/deploy-strategies/monitoring/deploy-strategies-dashboard.json
```

При импорте выбрать datasource **VictoriaMetrics**.

---

## Стратегия 1 — Rolling

Один Deployment `cookbook`. Kubernetes сам заменяет поды по одному.

- `maxSurge: 1` — на время обновления поднимается один дополнительный под;
- `maxUnavailable: 0` — старые поды не гасятся, пока новый не станет `Ready`.

`HTTPRoute` всё время смотрит на Service `cookbook`.

```bash
kubectl apply -f k8s/deploy-strategies/strategies/rolling/01-app-v1.yaml
kubectl rollout status deploy/cookbook -n cookbook-deploy

# в другом терминале
./task/task8/scripts/traffic.sh

# накатываем v2
kubectl apply -f k8s/deploy-strategies/strategies/rolling/02-app-v2.yaml

# наблюдаем замену подов
kubectl get rs -n cookbook-deploy -l app=cookbook --show-labels -w
kubectl rollout status deploy/cookbook -n cookbook-deploy
```

Ожидаемо:

- в трафике какое-то время чередуются `v1.0.0` и `v2.0.0`;
- затем остаются только поды `v2.0.0`;
- на графике — плавный спад v1 и рост v2.

Откат:

```bash
kubectl rollout undo deploy/cookbook -n cookbook-deploy
```

Трафик:

```bash
./task/task8/scripts/traffic.sh
```

---

## Стратегия 2 — Blue/Green

Обе версии подняты одновременно. Трафик переключается одним изменением
`HTTPRoute`.

```bash
./task/task8/scripts/reset.sh

kubectl apply -f k8s/deploy-strategies/app/app-v1.yaml
kubectl apply -f k8s/deploy-strategies/app/app-v2.yaml

# весь трафик на green (v1)
kubectl apply -f k8s/deploy-strategies/strategies/blue-green/httproute-green.yaml

# мгновенно на blue (v2)
kubectl apply -f k8s/deploy-strategies/strategies/blue-green/httproute-blue.yaml

# мгновенный откат
kubectl apply -f k8s/deploy-strategies/strategies/blue-green/httproute-green.yaml
```

Ожидаемо:

- при green трафик идёт только на `v1.0.0`;
- после blue — только на `v2.0.0`;
- на графике ступенька `0 → 100%`.

Трафик:

```bash
./task/task8/scripts/traffic.sh
```

---

## Стратегия 3 — Canary

Обе версии подняты. Доля трафика задаётся весами `backendRefs`.

```bash
./task/task8/scripts/reset.sh
kubectl apply -f k8s/deploy-strategies/app/app-v1.yaml
kubectl apply -f k8s/deploy-strategies/app/app-v2.yaml

# 90% v1 / 10% v2
kubectl apply -f k8s/deploy-strategies/strategies/canary/httproute-90-10.yaml
./task/task8/scripts/traffic.sh

# 50 / 50
kubectl apply -f k8s/deploy-strategies/strategies/canary/httproute-50-50.yaml
./task/task8/scripts/traffic.sh

# 0 / 100
kubectl apply -f k8s/deploy-strategies/strategies/canary/httproute-0-100.yaml
./task/task8/scripts/traffic.sh
```

После каждого шага остановить трафик через `Ctrl-C` и применить следующий
маршрут.

Ожидаемо:

- v2 получает примерно 10%, затем 50%, затем 100%;
- на графике — доли v2.

Откат на любом шаге — применить файл с меньшим весом v2.

---

## Стратегия 4 — A/B

На новую версию уходит не случайная доля, а выбранная аудитория — по заголовку
`x-user-group: beta`.

```bash
./task/task8/scripts/reset.sh
kubectl apply -f k8s/deploy-strategies/app/app-v1.yaml
kubectl apply -f k8s/deploy-strategies/app/app-v2.yaml
kubectl apply -f k8s/deploy-strategies/strategies/ab/httproute-ab.yaml
```

Проверка:

```bash
curl -sI http://localhost:8888/api/v1/recipes | grep -i x-app-version
# v1.0.0

curl -sI -H 'x-user-group: beta' http://localhost:8888/api/v1/recipes | grep -i x-app-version
# v2.0.0
```

Или через `traffic.sh`:

```bash
EXTRA_HEADER='x-user-group: beta' ./task/task8/scripts/traffic.sh
```

`localhost:8888` доступен, пока запущен `traffic.sh` или отдельный
`kubectl port-forward` на Envoy-прокси.

---

## Стратегия 5 — Shadow

Пользователь получает ответ от v1, а копия запроса зеркалируется на v2.

```bash
./task/task8/scripts/reset.sh
kubectl apply -f k8s/deploy-strategies/app/app-v1.yaml
kubectl apply -f k8s/deploy-strategies/app/app-v2.yaml
kubectl apply -f k8s/deploy-strategies/strategies/shadow/httproute-shadow.yaml

./task/task8/scripts/traffic.sh

kubectl logs -n cookbook-deploy -l app=cookbook,version=v2.0.0 --tail=50
```

Ожидаемо:

- `curl` и `traffic.sh` показывают ответы `v1.0.0`;
- в логах `v2.0.0` видны зеркалированные запросы;
- метрики v2 растут, но пользователь их не видит.

---

## Мониторинг

Дашборд:

```text
k8s/deploy-strategies/monitoring/deploy-strategies-dashboard.json
```

Импорт: **Dashboards → New → Import → Upload JSON**, затем выбрать datasource
**VictoriaMetrics**.

Запросы:

```promql
# RPS по версиям
sum(rate(http_server_requests_seconds_count{
  namespace="cookbook-deploy",
  application="cookbook",
  uri!~"/actuator.*"
}[2m])) by (version)
```

```promql
# Доля v2
100 *
sum(rate(http_server_requests_seconds_count{
  namespace="cookbook-deploy",
  application="cookbook",
  version="v2.0.0",
  uri!~"/actuator.*"
}[2m]))
/
sum(rate(http_server_requests_seconds_count{
  namespace="cookbook-deploy",
  application="cookbook",
  version=~"v1.0.0|v2.0.0",
  uri!~"/actuator.*"
}[2m]))
```

---

## Сброс и удаление

Перед переходом к следующей стратегии:

```bash
./task/task8/scripts/reset.sh
```

Скрипт удаляет только `HTTPRoute`, `Deployment` и `Service` приложений.
PostgreSQL, Gateway, ConfigMap и мониторинг остаются.

Полное удаление ресурсов задания:

```bash
./task/task8/scripts/teardown.sh
```

С удалением Envoy Gateway:

```bash
./task/task8/scripts/teardown.sh --envoy
```

`teardown.sh` удаляет namespace `cookbook-deploy`, но не трогает `cookbook`
с PostgreSQL, `db-secret` и мониторингом.

---

## Приложение app

`Deployment app` из `k8s/spring-boot/` остаётся в namespace `cookbook`.
`setup.sh` его не создаёт и не трогает. На стенд он не влияет: дашборд
фильтрует `namespace="cookbook-deploy"`, а Gateway `cookbook-deploy` ведёт
трафик только на `cookbook*`.
