# Приложение «Кулинарная книга» (Spring Boot) в minikube

Манифесты для развёртывания приложения из ДЗ №1 в namespace `cookbook`:
Spring Boot приложение (`app`) + PostgreSQL (`db`).

## Состав
| Файл | Объект | Назначение |
|------|--------|-----------|
| `00-namespace.yaml` | `Namespace cookbook` | пространство имён для всех ресурсов |
| `01-db-secret.yaml` | `Secret db-secret` | логин/пароль/имя БД PostgreSQL |
| `02-db-pvc.yaml` | `PersistentVolumeClaim db-pvc` | хранение данных БД (1Gi) |
| `03-db-deployment.yaml` | `Deployment db` | PostgreSQL 16 (`postgres:16-alpine`) |
| `04-db-service.yaml` | `Service db` | внутренний доступ к БД (ClusterIP:5432) |
| `05-app-configmap.yaml` | `ConfigMap app-config` | несекретные настройки (`DB_URL`, порт, профиль) |
| `06-app-deployment.yaml` | `Deployment app` | Spring Boot приложение (`cookbook-app:latest`) |
| `07-app-service.yaml` | `Service app` | внешний доступ к UI (NodePort 30080) |

## Сборка образа в minikube
```bash
eval $(minikube docker-env)
docker build -t cookbook-app:latest .
docker images | grep cookbook-app
```
`imagePullPolicy: IfNotPresent` в `06-app-deployment.yaml` — kubelet берёт локальный образ.

## Запуск
```bash
kubectl apply -f k8s/spring-boot/

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

## Масштабирование
```bash
kubectl scale deploy/app --replicas=2 -n cookbook
kubectl get pods -n cookbook
```
`app` — stateless (сессии в БД, Spring Session JDBC), поэтому масштабируется без sticky-сессий.
`db` масштабировать нельзя (один писатель, том `ReadWriteOnce`).

## Удаление
```bash
kubectl delete -f k8s/spring-boot/
```

## Связь с другими частями
- Доставка логов: `k8s/vector/` (DaemonSet Vector) → `k8s/victoria-logs/`.
- Приложение ходит в БД по DNS-имени сервиса `db:5432` (`DB_URL` из `ConfigMap app-config`).
