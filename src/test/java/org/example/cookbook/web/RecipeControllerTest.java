package org.example.cookbook.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.example.cookbook.config.SecurityConfig;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.service.IngredientService;
import org.example.cookbook.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipeController.class)
@Import(SecurityConfig.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private IngredientService ingredientService;

    @Test
    void guestSeesFeed() throws Exception {
        when(recipeService.search(isNull(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void guestCannotOpenNewRecipeForm() throws Exception {
        mockMvc.perform(get("/recipes/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void nonAuthorCannotEdit() throws Exception {
        when(recipeService.getForEdit(1L, "other")).thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/recipes/1/edit").with(user("other")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"));
    }

    @Test
    void authorCreatesRecipe() throws Exception {
        mockMvc.perform(post("/recipes").with(user("author")).with(csrf())
                        .param("title", "Блины")
                        .param("servings", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recipes"));

        verify(recipeService).create(any(RecipeForm.class), eq("author"));
    }
}
