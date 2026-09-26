package org.example.cookbook.dto;

import org.example.cookbook.domain.Recipe;

public record RecipeListItem(
        Long id,
        String title,
        String authorUsername,
        Integer cookingTimeMin,
        int servings
) {

    public static RecipeListItem from(Recipe recipe) {
        return new RecipeListItem(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getAuthor().getUsername(),
                recipe.getCookingTimeMin(),
                recipe.getServings()
        );
    }
}
