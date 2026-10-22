package org.example.cookbook.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.cookbook.config.SecurityConfig;
import org.example.cookbook.domain.Ingredient;
import org.example.cookbook.dto.IngredientForm;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.service.IngredientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngredientApiController.class)
@Import(SecurityConfig.class)
class IngredientApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    @Test
    @DisplayName("GET /api/v1/ingredients возвращает список ингредиентов в JSON")
    void listReturnsIngredients() throws Exception {
        when(ingredientService.findAll(isNull())).thenReturn(List.of(ingredient()));

        mockMvc.perform(get("/api/v1/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Соль"));
    }

    @Test
    @DisplayName("GET /api/v1/ingredients/{id} возвращает 404, если ингредиента нет")
    void getMissingReturnsNotFound() throws Exception {
        when(ingredientService.get(99L)).thenThrow(new NotFoundException("Ингредиент не найден: 99"));

        mockMvc.perform(get("/api/v1/ingredients/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/ingredients создаёт ингредиент и возвращает 201 с Location")
    void createReturnsCreated() throws Exception {
        when(ingredientService.nameExists(eq("Соль"), isNull())).thenReturn(false);
        when(ingredientService.create(any(IngredientForm.class))).thenReturn(ingredient());

        mockMvc.perform(post("/api/v1/ingredients")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Соль\",\"unit\":\"г\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ingredients/1"))
                .andExpect(jsonPath("$.name").value("Соль"));
    }

    @Test
    @DisplayName("POST /api/v1/ingredients с дубликатом названия возвращает 409")
    void createDuplicateReturnsConflict() throws Exception {
        when(ingredientService.nameExists(eq("Соль"), isNull())).thenReturn(true);

        mockMvc.perform(post("/api/v1/ingredients")
                        .with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Соль\",\"unit\":\"г\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/v1/ingredients/{id} возвращает 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/ingredients/1").with(user("admin")))
                .andExpect(status().isNoContent());
    }

    private Ingredient ingredient() {
        Ingredient ingredient = new Ingredient();
        ReflectionTestUtils.setField(ingredient, "id", 1L);
        ingredient.setName("Соль");
        ingredient.setUnit("г");
        return ingredient;
    }
}
