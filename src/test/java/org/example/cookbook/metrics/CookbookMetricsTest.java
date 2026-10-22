package org.example.cookbook.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CookbookMetricsTest {

    private SimpleMeterRegistry registry;
    private CookbookMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new CookbookMetrics(registry);
    }

    @Test
    @DisplayName("Все бизнес-счётчики регистрируются с нужными именами")
    void registersExpectedCounters() {
        metrics.userRegistered();
        metrics.recipeCreated();
        metrics.recipeUpdated();
        metrics.recipeDeleted();
        metrics.ingredientCreated();
        metrics.ingredientDeleted();
        metrics.loginSuccess();
        metrics.loginFailure();

        assertThat(registry.counter("cookbook.users.registered").count()).isEqualTo(1);
        assertThat(registry.counter("cookbook.recipes.created").count()).isEqualTo(1);
        assertThat(registry.counter("cookbook.recipes.updated").count()).isEqualTo(1);
        assertThat(registry.counter("cookbook.recipes.deleted").count()).isEqualTo(1);
        assertThat(registry.counter("cookbook.ingredients.created").count()).isEqualTo(1);
        assertThat(registry.counter("cookbook.ingredients.deleted").count()).isEqualTo(1);
        assertThat(registry.counter("cookbook.auth.logins", "result", "success").count()).isEqualTo(1);
        assertThat(registry.counter("cookbook.auth.logins", "result", "failure").count()).isEqualTo(1);
    }
}
