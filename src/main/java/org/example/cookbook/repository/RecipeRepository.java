package org.example.cookbook.repository;

import org.example.cookbook.domain.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Page<Recipe> findByAuthorUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
