# Кулинарная книга (Cookbook)

Клиент-серверное веб-приложение: общая кулинарная книга сообщества.
Любой зарегистрированный пользователь публикует свои рецепты и просматривает чужие;
редактировать и удалять рецепт может только его автор.

## Стек

- Java 21, Spring Boot 4.1.1
- Spring MVC + Thymeleaf + Bootstrap 5 (серверный рендеринг)
- Spring Data JPA (Hibernate), PostgreSQL 16
- Spring Security (form login), Spring Session JDBC (сессии в БД)
- Flyway (миграции схемы), Gradle (Kotlin DSL)
- Docker + docker-compose
- Тесты: JUnit, Mockito, MockMvc, Testcontainers

## Требования

- Docker и docker-compose
- JDK 21 (только для локального запуска без Docker)

## Переменные окружения

| Переменная | Назначение | По умолчанию |
|-----------|-----------|--------------|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/cookbook` |
| `DB_USER` | Пользователь БД | `cookbook` |
| `DB_PASSWORD` | Пароль БД | `cookbook` |
| `SERVER_PORT` | Порт приложения | `8080` |
| `SPRING_PROFILES_ACTIVE` | Профиль | `dev` |
| `LOG_LEVEL` | Уровень логирования | `INFO` |

## Запуск через Docker

```bash
cp .env.example .env
docker compose up --build
```

Приложение: http://localhost:8080

## Локальный запуск

```bash
docker compose up -d db
./gradlew bootRun
```

Приложение использует значения по умолчанию из `application.yaml`
(`jdbc:postgresql://localhost:5432/cookbook`).

## Тесты

```bash
./gradlew test                 # unit + web, без Docker
./gradlew test -Pintegration   # всё, включая интеграционные (Testcontainers)
```

## Страницы

- `/` , `/recipes` — лента рецептов с поиском по названию и пагинацией
- `/recipes/{id}` — карточка рецепта
- `/recipes/new`, `/recipes/{id}/edit` — форма создания/редактирования
- `/recipes/my` — мои рецепты
- `/users/{username}` — страница автора и его рецепты
- `/ingredients` — справочник ингредиентов
- `/login`, `/register`

## Роли

- **Гость** — просматривает рецепты, ингредиенты и страницы авторов.
- **Пользователь** — создаёт рецепты; редактирует и удаляет только свои.

## Замечание про порт 5432

Если локально работает системный PostgreSQL, он занимает порт 5432 и мешает
контейнеру. Остановите его (`sudo systemctl stop postgresql`) либо задайте другой
`DB_PORT` в `.env`.
