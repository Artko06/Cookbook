package org.example.cookbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.example.cookbook.domain.Ingredient;
import org.example.cookbook.domain.Recipe;
import org.example.cookbook.domain.User;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.dto.RecipeIngredientForm;
import org.example.cookbook.dto.RecipeListItem;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.repository.IngredientRepository;
import org.example.cookbook.repository.RecipeRepository;
import org.example.cookbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

class RecipeServiceTest {

    private RecipeRepository recipes;
    private UserRepository users;
    private IngredientRepository ingredients;
    private RecipeService service;

    @BeforeEach
    void setUp() {
        recipes = mock(RecipeRepository.class);
        users = mock(UserRepository.class);
        ingredients = mock(IngredientRepository.class);
        service = new RecipeService(recipes, users, ingredients);
    }

    @Test
    void getThrowsWhenRecipeMissing() {
        when(recipes.findDetailedById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsNonAuthor() {
        when(recipes.findDetailedById(1L)).thenReturn(Optional.of(recipeOf("author")));

        assertThatThrownBy(() -> service.update(1L, new RecipeForm(), "other"))
                .isInstanceOf(AccessDeniedException.class);
        verify(recipes, never()).save(any());
    }

    @Test
    void deleteRejectsNonAuthor() {
        when(recipes.findDetailedById(1L)).thenReturn(Optional.of(recipeOf("author")));

        assertThatThrownBy(() -> service.delete(1L, "other"))
                .isInstanceOf(AccessDeniedException.class);
        verify(recipes, never()).delete(any());
    }

    @Test
    void createStoresRecipeWithComposition() {
        User author = new User();
        author.setUsername("author");
        when(users.findByUsername("author")).thenReturn(Optional.of(author));

        Ingredient flour = new Ingredient();
        flour.setName("мука");
        flour.setUnit("г");
        when(ingredients.findById(5L)).thenReturn(Optional.of(flour));

        RecipeIngredientForm line = new RecipeIngredientForm();
        line.setIngredientId(5L);
        line.setQuantity(new BigDecimal("500"));
        line.setUnit("г");
        line.setNote("просеять");

        RecipeForm form = new RecipeForm();
        form.setTitle("Блины");
        form.setServings(3);
        form.setIngredients(List.of(line));

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        service.create(form, "author");
        verify(recipes).save(captor.capture());

        Recipe saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Блины");
        assertThat(saved.getServings()).isEqualTo(3);
        assertThat(saved.getAuthor()).isSameAs(author);
        assertThat(saved.getIngredients()).hasSize(1);
        assertThat(saved.getIngredients().get(0).getIngredient()).isSameAs(flour);
        assertThat(saved.getIngredients().get(0).getQuantity()).isEqualByComparingTo("500");
    }

    @Test
    void searchMapsRecipesToPageItems() {
        Recipe recipe = recipeOf("author");
        recipe.setTitle("Борщ");
        when(recipes.searchByTitle(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recipe)));

        Page<RecipeListItem> result = service.search(null, 0, 12);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Борщ");
        assertThat(result.getContent().get(0).authorUsername()).isEqualTo("author");
    }

    private Recipe recipeOf(String username) {
        User author = new User();
        author.setUsername(username);
        Recipe recipe = new Recipe();
        recipe.setTitle("Рецепт");
        recipe.setAuthor(author);
        return recipe;
    }
}
