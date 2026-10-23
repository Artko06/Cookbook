# Приложение «Кулинарная книга» (Spring Boot) в minikube

Манифесты для развёртывания приложения из ДЗ №1 в namespace `cookbook`:
Spring Boot приложение (`app`) + PostgreSQL (`db`).

## Состав
| Файл | Объект | Назначение |
|------|--------|-----------|
| `00-namespace.yaml` | `Namespace cookbook` | пространство имён для всех ресурсов |
| `01-db-pvc.yaml` | `PersistentVolumeClaim db-pvc` | хранение данных БД (1Gi) |
| `02-db-deployment.yaml` | `Deployment db` | PostgreSQL 16 (`postgres:16-alpine`) + sidecar `postgres-exporter` (порт 9187) |
| `03-db-service.yaml` | `Service db` | внутренний доступ к БД (ClusterIP:5432) |
| `04-app-configmap.yaml` | `ConfigMap app-config` | несекретные настройки (`DB_URL`, порт, профиль) |
| `05-app-deployment.yaml` | `Deployment app` | Spring Boot приложение (`cookbook-app:latest`) |
| `06-app-service.yaml` | `Service app` | внешний доступ к UI (NodePort 30080) |

`kustomization.yaml` собирает манифесты и генерирует `db-secret` из локального `db-secret.env` (не хранится в git).

## Сборка образа в minikube
```bash
eval $(minikube docker-env)
docker build -t cookbook-app:latest .
docker images | grep cookbook-app
```
`imagePullPolicy: IfNotPresent` в `05-app-deployment.yaml` — kubelet берёт локальный образ.

## Запуск

Создайте `k8s/spring-boot/db-secret.env`:

```env
POSTGRES_DB=cookbook
POSTGRES_USER=cookbook
POSTGRES_PASSWORD=cookbook
```

```bash
kubectl apply -k k8s/spring-boot/

# проверка
kubectl get pods,pvc,svc -n cookbook
kubectl get pods -n cookbook -w              # дождаться Running/Ready

# логи приложения (Flyway-миграции + старт)
kubectl logs deploy/app -n cookbook
```

## Доступ к UI
```bash
kubectl port-forward svc/app 8080:8080 -n cookbook   # http://localhost:8080
# или
minikube service app -n cookbook                     # NodePort 30080
```

## Метрики
```bash
# метрики приложения
kubectl port-forward deploy/app -n cookbook 8081:8081
http://localhost:8081/actuator/prometheus

# метрики PostgreSQL
kubectl port-forward deploy/db -n cookbook 9187:9187
http://localhost:9187/metrics
```

## Масштабирование
```bash
kubectl scale deploy/app --replicas=2 -n cookbook
kubectl get pods -n cookbook
```
`app` — stateless (сессии в БД, Spring Session JDBC), поэтому масштабируется без sticky-сессий.
`db` масштабировать нельзя (один писатель, том `ReadWriteOnce`).

## Удаление
```bash
kubectl delete -k k8s/spring-boot/
```

## Связь с другими частями
- Доставка логов: `k8s/vector/` (DaemonSet Vector) → `k8s/victoria-logs/`.
- Приложение ходит в БД по DNS-имени сервиса `db:5432` (`DB_URL` из `ConfigMap app-config`).
