# vmalert + Alertmanager в minikube

Алерты для приложения Cookbook по метрикам VictoriaMetrics. `vmalert` вычисляет
правила, Alertmanager отправляет уведомления в Telegram. Работает в namespace
`monitoring`.

## Состав
- `vmalert-rules.yaml` — `ConfigMap vmalert-rules` с 4 правилами (ключ `alert-rules.yaml`);
- `kustomization.yaml` — применяет ConfigMap и генерирует `Secret alertmanager-telegram`
  из локального `telegram-secret.env`;
- `values-alert.yaml` — Helm values для чарта `vm/victoria-metrics-alert`:
  datasource на `vm-single`, Alertmanager, Telegram receiver, монтирование Secret;
- `telegram-secret.env.example` — пример с плейсхолдерами;
- `telegram-secret.env` — реальные `bot-token` и `chat-id`, не хранится в git.

`vmalert` и Alertmanager ставятся Helm-чартом, сама VictoriaMetrics не
переустанавливается — используется существующий `vm-single`.

## Алерты
| Алерт | Условие | for | Severity |
|---|---|---|---|
| `CookbookAppDown` | `up{app="app"} == 0` или серия отсутствует | 2m | critical |
| `CookbookProbeFailures` | доля неуспешных пробера `> 20%` | 5m | warning |
| `CookbookDbDown` | `pg_up{app="db"} == 0` или серия отсутствует | 2m | critical |
| `CookbookHigh5xxRate` | доля 5xx среди пользовательских запросов `> 10%` | 5m | critical |

## Telegram

1. В Telegram: `@BotFather` → `/newbot` → получить токен.
2. Написать боту сообщение.
3. Узнать chat id:
   `https://api.telegram.org/bot<TOKEN>/getUpdates`
4. Создать локальный env-файл:
   ```bash
   cp k8s/alerting/telegram-secret.env.example k8s/alerting/telegram-secret.env
   ```
   ```env
   bot-token=<TOKEN>
   chat-id=<CHAT_ID>
   ```

## Запуск

Сначала создаём ConfigMap и Secret, затем ставим Helm-релиз:

```bash
kubectl apply -k k8s/alerting/

helm repo add vm https://victoriametrics.github.io/helm-charts/
helm repo update

helm upgrade --install vm-alert vm/victoria-metrics-alert \
  -n monitoring -f k8s/alerting/values-alert.yaml
```

Если репозиторий `vm` уже добавлен, `helm repo add` можно пропустить.

## Проверка

```bash
kubectl port-forward -n monitoring \
  deploy/vm-alert-victoria-metrics-alert-server 8880:8880
```

```bash
kubectl port-forward -n monitoring \
  deploy/vm-alert-victoria-metrics-alert-alertmanager 9093:9093
```

- vmalert: `http://localhost:8880` — 4 правила;
- Alertmanager: `http://localhost:9093`;
- API:
  - `http://localhost:8880/api/v1/rules`
  - `http://localhost:8880/api/v1/alerts`
  - `http://localhost:9093/api/v2/alerts`

## Демонстрация

### Подготовка

Образ `cookbook-app:latest` должен быть собран заранее
(см. `k8s/spring-boot/README.md`).

```bash
kubectl apply -k k8s/spring-boot/
kubectl rollout status deploy/db -n cookbook
kubectl rollout status deploy/app -n cookbook

kubectl port-forward svc/app 8080:8080 -n cookbook
# http://localhost:8080
```

`kubectl port-forward` должен работать всё время демонстрации.

### Chaos

```bash
kubectl set env deploy/app -n cookbook DEMO_CHAOS_ENABLED=true
kubectl rollout status deploy/app -n cookbook

curl -X POST 'http://localhost:8080/api/v1/chaos?errorRate=0.75'
```

Через 5–7 минут сработают `CookbookHigh5xxRate` и `CookbookProbeFailures`.
Восстановление:

```bash
curl -X POST 'http://localhost:8080/api/v1/chaos/reset'
```

Проверка `CookbookAppDown`:

```bash
kubectl scale deploy/app -n cookbook --replicas=0
# подождать 2–3 минуты
kubectl scale deploy/app -n cookbook --replicas=1
kubectl rollout status deploy/app -n cookbook
```

## Удаление
```bash
helm uninstall vm-alert -n monitoring
kubectl delete -k k8s/alerting/
```
