package org.example.cookbook.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CookbookMetrics {

    private final Counter recipesCreated;
    private final Counter recipesUpdated;
    private final Counter recipesDeleted;
    private final Counter ingredientsCreated;
    private final Counter ingredientsDeleted;
    private final Counter usersRegistered;
    private final Counter loginSuccess;
    private final Counter loginFailure;

    public CookbookMetrics(MeterRegistry registry) {
        this.recipesCreated = Counter.builder("cookbook.recipes.created")
                .description("Успешно созданные рецепты")
                .register(registry);
        this.recipesUpdated = Counter.builder("cookbook.recipes.updated")
                .description("Успешно обновлённые рецепты")
                .register(registry);
        this.recipesDeleted = Counter.builder("cookbook.recipes.deleted")
                .description("Успешно удалённые рецепты")
                .register(registry);
        this.ingredientsCreated = Counter.builder("cookbook.ingredients.created")
                .description("Успешно созданные ингредиенты")
                .register(registry);
        this.ingredientsDeleted = Counter.builder("cookbook.ingredients.deleted")
                .description("Успешно удалённые ингредиенты")
                .register(registry);
        this.usersRegistered = Counter.builder("cookbook.users.registered")
                .description("Успешно зарегистрированные пользователи")
                .register(registry);
        this.loginSuccess = Counter.builder("cookbook.auth.logins")
                .description("Успешные попытки входа")
                .tag("result", "success")
                .register(registry);
        this.loginFailure = Counter.builder("cookbook.auth.logins")
                .description("Неудачные попытки входа")
                .tag("result", "failure")
                .register(registry);
    }

    public void userRegistered() {
        usersRegistered.increment();
    }

    public void loginSuccess() {
        loginSuccess.increment();
    }

    public void loginFailure() {
        loginFailure.increment();
    }

    public void recipeCreated() {
        recipesCreated.increment();
    }

    public void recipeUpdated() {
        recipesUpdated.increment();
    }

    public void recipeDeleted() {
        recipesDeleted.increment();
    }

    public void ingredientCreated() {
        ingredientsCreated.increment();
    }

    public void ingredientDeleted() {
        ingredientsDeleted.increment();
    }
}
