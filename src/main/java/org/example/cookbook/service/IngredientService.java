package org.example.cookbook.service;

import java.util.List;
import org.example.cookbook.domain.Ingredient;
import org.example.cookbook.dto.IngredientForm;
import org.example.cookbook.exception.IngredientInUseException;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.repository.IngredientRepository;
import org.example.cookbook.repository.RecipeIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {

    private final IngredientRepository ingredients;
    private final RecipeIngredientRepository recipeIngredients;

    public IngredientService(IngredientRepository ingredients, RecipeIngredientRepository recipeIngredients) {
        this.ingredients = ingredients;
        this.recipeIngredients = recipeIngredients;
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
    public void create(IngredientForm form) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(form.getName().trim());
        ingredient.setUnit(form.getUnit().trim());
        ingredients.save(ingredient);
    }

    @Transactional
    public void update(Long id, IngredientForm form) {
        Ingredient ingredient = get(id);
        ingredient.setName(form.getName().trim());
        ingredient.setUnit(form.getUnit().trim());
        ingredients.save(ingredient);
    }

    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = get(id);
        if (recipeIngredients.existsByIngredientId(id)) {
            throw new IngredientInUseException(
                    "Ингредиент «" + ingredient.getName() + "» используется в рецептах и не может быть удалён");
        }
        ingredients.delete(ingredient);
    }
}
