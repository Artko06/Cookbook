package org.example.cookbook.dto;

import java.math.BigDecimal;
import org.example.cookbook.domain.RecipeIngredient;

public record RecipeIngredientResponse(
        Long ingredientId,
        String ingredientName,
        String unit,
        BigDecimal quantity,
        String note
) {

    public static RecipeIngredientResponse from(RecipeIngredient line) {
        return new RecipeIngredientResponse(
                line.getIngredient().getId(),
                line.getIngredient().getName(),
                line.getUnit(),
                line.getQuantity(),
                line.getNote()
        );
    }
}
