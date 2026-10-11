package org.example.cookbook.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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
import org.example.cookbook.domain.Recipe;
import org.example.cookbook.domain.User;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.service.RecipeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipeApiController.class)
@Import(SecurityConfig.class)
class RecipeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    @DisplayName("GET /api/v1/recipes возвращает страницу рецептов в JSON")
    void listReturnsRecipes() throws Exception {
        when(recipeService.search(isNull(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{id} возвращает рецепт в JSON")
    void getReturnsRecipe() throws Exception {
        when(recipeService.get(1L)).thenReturn(recipe());

        mockMvc.perform(get("/api/v1/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Борщ"))
                .andExpect(jsonPath("$.authorUsername").value("author"));
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{id} возвращает 404, если рецепта нет")
    void getMissingReturnsNotFound() throws Exception {
        when(recipeService.get(99L)).thenThrow(new NotFoundException("Рецепт не найден: 99"));

        mockMvc.perform(get("/api/v1/recipes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/recipes создаёт рецепт и возвращает 201 с Location")
    void createReturnsCreated() throws Exception {
        when(recipeService.create(any(RecipeForm.class), eq("author"))).thenReturn(recipe());

        mockMvc.perform(post("/api/v1/recipes").with(user("author"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Борщ\",\"servings\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/recipes/1"))
                .andExpect(jsonPath("$.title").value("Борщ"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes без аутентификации возвращает 401")
    void createWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Борщ\",\"servings\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/recipes с невалидным телом возвращает 400")
    void createWithInvalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/recipes").with(user("author"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"servings\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{id} для не автора возвращает 403")
    void deleteNonAuthorReturnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("denied")).when(recipeService).delete(1L, "other");

        mockMvc.perform(delete("/api/v1/recipes/1").with(user("other")))
                .andExpect(status().isForbidden());
    }

    private Recipe recipe() {
        User author = new User();
        author.setUsername("author");
        Recipe recipe = new Recipe();
        ReflectionTestUtils.setField(recipe, "id", 1L);
        recipe.setTitle("Борщ");
        recipe.setServings(2);
        recipe.setAuthor(author);
        return recipe;
    }
}
