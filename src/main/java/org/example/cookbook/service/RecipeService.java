package org.example.cookbook.service;

import java.util.List;
import org.example.cookbook.domain.Recipe;
import org.example.cookbook.domain.User;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.dto.RecipeListItem;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.repository.RecipeRepository;
import org.example.cookbook.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private final RecipeRepository recipes;
    private final UserRepository users;

    public RecipeService(RecipeRepository recipes, UserRepository users) {
        this.recipes = recipes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<RecipeListItem> feed() {
        return recipes.findAllByOrderByCreatedAtDesc().stream()
                .map(RecipeListItem::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Recipe get(Long id) {
        return recipes.findWithAuthorById(id)
                .orElseThrow(() -> new NotFoundException("Рецепт не найден: " + id));
    }

    @Transactional(readOnly = true)
    public Recipe getForEdit(Long id, String username) {
        Recipe recipe = get(id);
        requireAuthor(recipe, username);
        return recipe;
    }

    @Transactional
    public Recipe create(RecipeForm form, String username) {
        User author = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + username));
        Recipe recipe = new Recipe();
        apply(recipe, form);
        recipe.setAuthor(author);
        return recipes.save(recipe);
    }

    @Transactional
    public Recipe update(Long id, RecipeForm form, String username) {
        Recipe recipe = get(id);
        requireAuthor(recipe, username);
        apply(recipe, form);
        return recipes.save(recipe);
    }

    @Transactional
    public void delete(Long id, String username) {
        Recipe recipe = get(id);
        requireAuthor(recipe, username);
        recipes.delete(recipe);
    }

    private void apply(Recipe recipe, RecipeForm form) {
        recipe.setTitle(form.getTitle().trim());
        recipe.setDescription(blankToNull(form.getDescription()));
        recipe.setInstructions(blankToNull(form.getInstructions()));
        recipe.setServings(form.getServings());
        recipe.setCookingTimeMin(form.getCookingTimeMin());
    }

    private void requireAuthor(Recipe recipe, String username) {
        if (username == null || !recipe.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("Редактировать и удалять рецепт может только его автор");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
