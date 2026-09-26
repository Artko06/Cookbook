package org.example.cookbook.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "Укажите логин")
    @Size(min = 3, max = 50, message = "Логин должен быть от 3 до 50 символов")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Логин может содержать только буквы, цифры и _")
    private String username;

    @NotBlank(message = "Укажите email")
    @Email(message = "Некорректный email")
    @Size(max = 120, message = "Email слишком длинный")
    private String email;

    @NotBlank(message = "Укажите пароль")
    @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
    private String password;

    @NotBlank(message = "Повторите пароль")
    private String confirmPassword;

    @AssertTrue(message = "Пароли не совпадают")
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
