package org.example.cookbook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class RecipeForm {

    @NotBlank(message = "Укажите название")
    @Size(max = 150, message = "Название не должно превышать 150 символов")
    private String title;

    private String description;

    private String instructions;

    @Min(value = 1, message = "Порций должно быть не меньше 1")
    private int servings = 1;

    @Min(value = 1, message = "Время готовки должно быть не меньше 1 минуты")
    private Integer cookingTimeMin;

    @Valid
    private List<RecipeIngredientForm> ingredients = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public Integer getCookingTimeMin() {
        return cookingTimeMin;
    }

    public void setCookingTimeMin(Integer cookingTimeMin) {
        this.cookingTimeMin = cookingTimeMin;
    }

    public List<RecipeIngredientForm> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredientForm> ingredients) {
        this.ingredients = ingredients;
    }
}
