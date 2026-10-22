package org.example.cookbook.dto;

import org.example.cookbook.domain.Ingredient;

public record IngredientResponse(Long id, String name, String unit) {

    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getUnit());
    }
}
