package org.example.cookbook.repository;

import java.util.Optional;
import org.example.cookbook.domain.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @EntityGraph(attributePaths = {"author", "ingredients", "ingredients.ingredient"})
    Optional<Recipe> findWithAuthorById(Long id);

    @EntityGraph(attributePaths = "author")
    @Query(value = "select r from Recipe r where lower(r.title) like lower(concat('%', :q, '%'))",
            countQuery = "select count(r) from Recipe r where lower(r.title) like lower(concat('%', :q, '%'))")
    Page<Recipe> searchByTitle(@Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Recipe> findByAuthorUsername(String username, Pageable pageable);
}
