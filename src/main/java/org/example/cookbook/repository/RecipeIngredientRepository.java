package org.example.cookbook.repository;

import org.example.cookbook.domain.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    boolean existsByIngredientId(Long ingredientId);
}
