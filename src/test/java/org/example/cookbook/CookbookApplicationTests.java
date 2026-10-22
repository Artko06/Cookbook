package org.example.cookbook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class CookbookApplicationTests extends AbstractIntegrationTest {

    @Test
    @DisplayName("Контекст приложения успешно загружается")
    void contextLoads() {
    }
}
