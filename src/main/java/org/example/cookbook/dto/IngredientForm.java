package org.example.cookbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IngredientForm {

    @NotBlank(message = "Укажите название")
    @Size(max = 100, message = "Название не должно превышать 100 символов")
    private String name;

    @NotBlank(message = "Укажите единицу измерения")
    @Size(max = 20, message = "Единица измерения не должна превышать 20 символов")
    private String unit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
