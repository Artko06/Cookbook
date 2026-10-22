package org.example.cookbook.service;

import java.util.List;
import org.example.cookbook.domain.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.cookbook.dto.IngredientForm;
import org.example.cookbook.exception.IngredientInUseException;
import org.example.cookbook.metrics.CookbookMetrics;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.repository.IngredientRepository;
import org.example.cookbook.repository.RecipeIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {

    private static final Logger log = LoggerFactory.getLogger(IngredientService.class);

    private final IngredientRepository ingredients;
    private final RecipeIngredientRepository recipeIngredients;
    private final CookbookMetrics metrics;

    public IngredientService(IngredientRepository ingredients, RecipeIngredientRepository recipeIngredients, CookbookMetrics metrics) {
        this.ingredients = ingredients;
        this.recipeIngredients = recipeIngredients;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<Ingredient> findAll(String query) {
        if (query == null || query.isBlank()) {
            return ingredients.findAllByOrderByNameAsc();
        }
        return ingredients.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim());
    }

    @Transactional(readOnly = true)
    public Ingredient get(Long id) {
        return ingredients.findById(id)
                .orElseThrow(() -> new NotFoundException("Ингредиент не найден: " + id));
    }

    @Transactional(readOnly = true)
    public boolean nameExists(String name, Long excludeId) {
        return ingredients.findByNameIgnoreCase(name == null ? "" : name.trim())
                .filter(found -> !found.getId().equals(excludeId))
                .isPresent();
    }

    @Transactional
    public Ingredient create(IngredientForm form) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(form.getName().trim());
        ingredient.setUnit(form.getUnit().trim());
        ingredients.save(ingredient);
        log.info("Ingredient created id={} name={}", ingredient.getId(), ingredient.getName());
        metrics.ingredientCreated();
        return ingredient;
    }

    @Transactional
    public Ingredient update(Long id, IngredientForm form) {
        Ingredient ingredient = get(id);
        ingredient.setName(form.getName().trim());
        ingredient.setUnit(form.getUnit().trim());
        ingredients.save(ingredient);
        log.info("Ingredient updated id={} name={}", ingredient.getId(), ingredient.getName());
        return ingredient;
    }

    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = get(id);
        if (recipeIngredients.existsByIngredientId(id)) {
            log.warn("Ingredient delete blocked id={} name={}", id, ingredient.getName());
            throw new IngredientInUseException(
                    "Ингредиент «" + ingredient.getName() + "» используется в рецептах и не может быть удалён");
        }
        ingredients.delete(ingredient);
        log.info("Ingredient deleted id={} name={}", id, ingredient.getName());
        metrics.ingredientDeleted();
    }
}
