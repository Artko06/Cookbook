package org.example.cookbook.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class RecipeIngredientForm {

    private Long ingredientId;

    @NotNull(message = "Укажите количество")
    @DecimalMin(value = "0.01", message = "Количество должно быть больше 0")
    private BigDecimal quantity;

    @Size(max = 20, message = "Единица измерения не должна превышать 20 символов")
    private String unit;

    @Size(max = 255, message = "Примечание не должно превышать 255 символов")
    private String note;

    public Long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Long ingredientId) {
        this.ingredientId = ingredientId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
