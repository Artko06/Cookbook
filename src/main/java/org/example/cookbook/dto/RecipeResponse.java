package org.example.cookbook.dto;

import java.util.List;
import org.example.cookbook.domain.Recipe;

public record RecipeResponse(
        Long id,
        String title,
        String description,
        String instructions,
        int servings,
        Integer cookingTimeMin,
        String authorUsername,
        List<RecipeIngredientResponse> ingredients
) {

    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getInstructions(),
                recipe.getServings(),
                recipe.getCookingTimeMin(),
                recipe.getAuthor().getUsername(),
                recipe.getIngredients().stream().map(RecipeIngredientResponse::from).toList()
        );
    }
}
