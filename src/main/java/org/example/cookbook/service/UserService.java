package org.example.cookbook.service;

import org.example.cookbook.domain.User;
import org.example.cookbook.metrics.CookbookMetrics;
import org.example.cookbook.dto.RegisterForm;
import org.example.cookbook.exception.NotFoundException;
import org.example.cookbook.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CookbookMetrics metrics;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, CookbookMetrics metrics) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.metrics = metrics;
    }

    @Transactional
    public void register(RegisterForm form) {
        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        users.save(user);
        metrics.userRegistered();
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return users.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return users.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + username));
    }
}
