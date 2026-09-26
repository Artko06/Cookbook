package org.example.cookbook.web;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.example.cookbook.domain.Recipe;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.dto.RecipeIngredientForm;
import org.example.cookbook.service.IngredientService;
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
    private final IngredientService ingredientService;

    public RecipeController(RecipeService recipeService, IngredientService ingredientService) {
        this.recipeService = recipeService;
        this.ingredientService = ingredientService;
    }

    @GetMapping({"/", "/recipes"})
    public String feed(Model model) {
        model.addAttribute("recipes", recipeService.feed());
        return "index";
    }

    @GetMapping("/recipes/new")
    public String createForm(@ModelAttribute("recipeForm") RecipeForm form, Model model) {
        prepareForm(model, "/recipes", null);
        return "recipes/form";
    }

    @PostMapping("/recipes")
    public String create(@Valid @ModelAttribute("recipeForm") RecipeForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model) {
        rejectDuplicateIngredients(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(model, "/recipes", null);
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
        model.addAttribute("recipeForm", toForm(recipe));
        prepareForm(model, "/recipes/" + id, id);
        return "recipes/form";
    }

    @PostMapping("/recipes/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("recipeForm") RecipeForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model) {
        rejectDuplicateIngredients(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareForm(model, "/recipes/" + id, id);
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

    private void prepareForm(Model model, String formAction, Long recipeId) {
        model.addAttribute("formAction", formAction);
        model.addAttribute("recipeId", recipeId);
        model.addAttribute("allIngredients", ingredientService.findAll(null));
    }

    private RecipeForm toForm(Recipe recipe) {
        RecipeForm form = new RecipeForm();
        form.setTitle(recipe.getTitle());
        form.setDescription(recipe.getDescription());
        form.setInstructions(recipe.getInstructions());
        form.setServings(recipe.getServings());
        form.setCookingTimeMin(recipe.getCookingTimeMin());
        List<RecipeIngredientForm> lines = new ArrayList<>();
        recipe.getIngredients().forEach(line -> {
            RecipeIngredientForm formLine = new RecipeIngredientForm();
            formLine.setIngredientId(line.getIngredient().getId());
            formLine.setQuantity(line.getQuantity());
            formLine.setUnit(line.getUnit());
            formLine.setNote(line.getNote());
            lines.add(formLine);
        });
        form.setIngredients(lines);
        return form;
    }

    private void rejectDuplicateIngredients(RecipeForm form, BindingResult bindingResult) {
        if (form.getIngredients() == null) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (RecipeIngredientForm line : form.getIngredients()) {
            if (line == null || line.getIngredientId() == null) {
                continue;
            }
            if (!seen.add(line.getIngredientId())) {
                bindingResult.reject("duplicateIngredients", "Ингредиент не должен повторяться в составе");
                return;
            }
        }
    }

    private boolean isAuthor(Recipe recipe, Authentication authentication) {
        return authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && recipe.getAuthor().getUsername().equals(authentication.getName());
    }
}
