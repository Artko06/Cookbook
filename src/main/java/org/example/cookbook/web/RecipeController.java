package org.example.cookbook.web;

import jakarta.validation.Valid;
import org.example.cookbook.domain.Recipe;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.service.RecipeService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping({"/", "/recipes"})
    public String feed(Model model) {
        model.addAttribute("recipes", recipeService.feed());
        return "index";
    }

    @GetMapping("/recipes/new")
    public String createForm(@ModelAttribute("recipeForm") RecipeForm form, Model model) {
        model.addAttribute("formAction", "/recipes");
        return "recipes/form";
    }

    @PostMapping("/recipes")
    public String create(@Valid @ModelAttribute("recipeForm") RecipeForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/recipes");
            return "recipes/form";
        }
        recipeService.create(form, authentication.getName());
        return "redirect:/recipes";
    }

    @GetMapping("/recipes/{id}")
    public String view(@PathVariable Long id, Authentication authentication, Model model) {
        Recipe recipe = recipeService.get(id);
        model.addAttribute("recipe", recipe);
        model.addAttribute("canEdit", isAuthor(recipe, authentication));
        return "recipes/view";
    }

    @GetMapping("/recipes/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        Recipe recipe = recipeService.getForEdit(id, authentication.getName());
        RecipeForm form = new RecipeForm();
        form.setTitle(recipe.getTitle());
        form.setDescription(recipe.getDescription());
        form.setInstructions(recipe.getInstructions());
        form.setServings(recipe.getServings());
        form.setCookingTimeMin(recipe.getCookingTimeMin());
        model.addAttribute("recipeForm", form);
        model.addAttribute("formAction", "/recipes/" + id);
        model.addAttribute("recipeId", id);
        return "recipes/form";
    }

    @PostMapping("/recipes/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("recipeForm") RecipeForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/recipes/" + id);
            model.addAttribute("recipeId", id);
            return "recipes/form";
        }
        recipeService.update(id, form, authentication.getName());
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/recipes/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        recipeService.delete(id, authentication.getName());
        return "redirect:/recipes";
    }

    private boolean isAuthor(Recipe recipe, Authentication authentication) {
        return authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && recipe.getAuthor().getUsername().equals(authentication.getName());
    }
}
