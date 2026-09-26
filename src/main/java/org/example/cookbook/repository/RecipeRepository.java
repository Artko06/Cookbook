package org.example.cookbook.repository;

import java.util.List;
import java.util.Optional;
import org.example.cookbook.domain.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @EntityGraph(attributePaths = {"author", "ingredients", "ingredients.ingredient"})
    Optional<Recipe> findWithAuthorById(Long id);

    List<Recipe> findAllByOrderByCreatedAtDesc();

    Page<Recipe> findByAuthorUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
