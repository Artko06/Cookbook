package org.example.cookbook.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.cookbook.dto.RegisterForm;
import org.example.cookbook.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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
            log.warn("Registration rejected username={} errors={}", form.getUsername(), bindingResult.getErrorCount());
            return "register";
        }

        userService.register(form);
        log.info("User registered username={}", form.getUsername());

        try {
            request.login(form.getUsername(), form.getPassword());
        } catch (ServletException ex) {
            log.warn("Auto-login after registration failed username={}", form.getUsername());
            return "redirect:/login";
        }
        return "redirect:/";
    }
}
