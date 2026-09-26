package org.example.cookbook.repository;

import java.util.List;
import org.example.cookbook.domain.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    List<RecipeIngredient> findByRecipeId(Long recipeId);

    boolean existsByIngredientId(Long ingredientId);
}
