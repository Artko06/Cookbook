package org.example.cookbook.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.cookbook.dto.RegisterForm;
import org.example.cookbook.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(@ModelAttribute("registerForm") RegisterForm form) {
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           HttpServletRequest request) {
        if (userService.usernameExists(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Логин уже занят");
        }
        if (userService.emailExists(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "Email уже занят");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        userService.register(form);

        try {
            request.login(form.getUsername(), form.getPassword());
        } catch (ServletException ex) {
            return "redirect:/login";
        }
        return "redirect:/";
    }
}
