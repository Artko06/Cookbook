package org.example.cookbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.cookbook.domain.Ingredient;
import org.example.cookbook.dto.IngredientForm;
import org.example.cookbook.exception.IngredientInUseException;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.repository.IngredientRepository;
import org.example.cookbook.repository.RecipeIngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IngredientServiceTest {

    private IngredientRepository ingredients;
    private RecipeIngredientRepository recipeIngredients;
    private IngredientService service;

    @BeforeEach
    void setUp() {
        ingredients = mock(IngredientRepository.class);
        recipeIngredients = mock(RecipeIngredientRepository.class);
        service = new IngredientService(ingredients, recipeIngredients);
    }

    @Test
    void getThrowsWhenIngredientMissing() {
        when(ingredients.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectsIngredientUsedInRecipes() {
        Ingredient ingredient = ingredient("мука", "г");
        when(ingredients.findById(1L)).thenReturn(Optional.of(ingredient));
        when(recipeIngredients.existsByIngredientId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(IngredientInUseException.class);
        verify(ingredients, never()).delete(any());
    }

    @Test
    void deleteRemovesUnusedIngredient() {
        Ingredient ingredient = ingredient("мука", "г");
        when(ingredients.findById(1L)).thenReturn(Optional.of(ingredient));
        when(recipeIngredients.existsByIngredientId(1L)).thenReturn(false);

        service.delete(1L);

        verify(ingredients).delete(ingredient);
    }

    @Test
    void findAllWithoutQueryReturnsAllSorted() {
        service.findAll(null);

        verify(ingredients).findAllByOrderByNameAsc();
    }

    @Test
    void findAllWithQueryTrimsAndSearches() {
        service.findAll("  му  ");

        verify(ingredients).findByNameContainingIgnoreCaseOrderByNameAsc("му");
    }

    @Test
    void createTrimsAndSaves() {
        IngredientForm form = new IngredientForm();
        form.setName("  Соль  ");
        form.setUnit("  г ");
        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);

        service.create(form);

        verify(ingredients).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Соль");
        assertThat(captor.getValue().getUnit()).isEqualTo("г");
    }

    private Ingredient ingredient(String name, String unit) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setUnit(unit);
        return ingredient;
    }
}
