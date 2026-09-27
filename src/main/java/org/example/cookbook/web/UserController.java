package org.example.cookbook.web;

import org.example.cookbook.dto.RecipeListItem;
import org.example.cookbook.service.RecipeService;
import org.example.cookbook.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private final UserService userService;
    private final RecipeService recipeService;

    public UserController(UserService userService, RecipeService recipeService) {
        this.userService = userService;
        this.recipeService = recipeService;
    }

    @GetMapping("/users/{username}")
    public String view(@PathVariable String username,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "size", defaultValue = "12") int size,
                       Model model) {
        model.addAttribute("author", userService.getByUsername(username));
        Page<RecipeListItem> result = recipeService.byAuthor(username, page, size);
        model.addAttribute("recipes", result.getContent());
        model.addAttribute("page", result);
        model.addAttribute("size", size);
        return "users/view";
    }
}
