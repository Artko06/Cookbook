package org.example.cookbook.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.cookbook.domain.Ingredient;
import org.example.cookbook.domain.Recipe;
import org.example.cookbook.domain.RecipeIngredient;
import org.example.cookbook.domain.User;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.dto.RecipeIngredientForm;
import org.example.cookbook.dto.RecipeListItem;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.repository.IngredientRepository;
import org.example.cookbook.repository.RecipeRepository;
import org.example.cookbook.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private final RecipeRepository recipes;
    private final UserRepository users;
    private final IngredientRepository ingredients;

    public RecipeService(RecipeRepository recipes, UserRepository users, IngredientRepository ingredients) {
        this.recipes = recipes;
        this.users = users;
        this.ingredients = ingredients;
    }

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    public Page<RecipeListItem> search(String q, int page, int size) {
        String query = q == null ? "" : q.trim();
        return recipes.searchByTitle(query, pageable(page, size))
                .map(RecipeListItem::from);
    }

    @Transactional(readOnly = true)
    public Page<RecipeListItem> byAuthor(String username, int page, int size) {
        return recipes.findByAuthorUsername(username, pageable(page, size))
                .map(RecipeListItem::from);
    }

    private Pageable pageable(int page, int size) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
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
        applyComposition(recipe, form.getIngredients());
        return recipes.save(recipe);
    }

    @Transactional
    public Recipe update(Long id, RecipeForm form, String username) {
        Recipe recipe = get(id);
        requireAuthor(recipe, username);
        apply(recipe, form);
        applyComposition(recipe, form.getIngredients());
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

    private void applyComposition(Recipe recipe, List<RecipeIngredientForm> formLines) {
        List<RecipeIngredientForm> lines = formLines == null ? List.of() : formLines;

        Set<Long> targetIds = lines.stream()
                .filter(line -> line != null && line.getIngredientId() != null)
                .map(RecipeIngredientForm::getIngredientId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, RecipeIngredient> current = recipe.getIngredients().stream()
                .collect(Collectors.toMap(line -> line.getIngredient().getId(), line -> line));

        recipe.getIngredients().removeIf(line -> !targetIds.contains(line.getIngredient().getId()));

        for (RecipeIngredientForm formLine : lines) {
            if (formLine == null || formLine.getIngredientId() == null) {
                continue;
            }
            RecipeIngredient line = current.get(formLine.getIngredientId());
            if (line == null) {
                Ingredient ingredient = ingredients.findById(formLine.getIngredientId())
                        .orElseThrow(() -> new NotFoundException("Ингредиент не найден: " + formLine.getIngredientId()));
                line = new RecipeIngredient();
                line.setIngredient(ingredient);
                recipe.addIngredient(line);
            }
            line.setQuantity(formLine.getQuantity());
            line.setUnit(blankToNull(formLine.getUnit()));
            line.setNote(blankToNull(formLine.getNote()));
        }
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
