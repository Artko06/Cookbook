package org.example.cookbook.repository;

import java.util.List;
import java.util.Optional;
import org.example.cookbook.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findAllByOrderByNameAsc();

    List<Ingredient> findByNameContainingIgnoreCaseOrderByNameAsc(String query);

    Optional<Ingredient> findByNameIgnoreCase(String name);
}
