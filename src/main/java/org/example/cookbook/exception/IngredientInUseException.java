package org.example.cookbook.exception;

public class IngredientInUseException extends RuntimeException {

    public IngredientInUseException(String message) {
        super(message);
    }
}
