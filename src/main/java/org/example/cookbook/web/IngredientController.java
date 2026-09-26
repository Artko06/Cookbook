package org.example.cookbook.web;

import jakarta.validation.Valid;
import org.example.cookbook.domain.Ingredient;
import org.example.cookbook.dto.IngredientForm;
import org.example.cookbook.service.IngredientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("ingredients", ingredientService.findAll(q));
        model.addAttribute("q", q);
        return "ingredients/list";
    }

    @GetMapping("/new")
    public String createForm(@ModelAttribute("ingredientForm") IngredientForm form, Model model) {
        model.addAttribute("formAction", "/ingredients");
        return "ingredients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ingredientForm") IngredientForm form,
                         BindingResult bindingResult,
                         Model model) {
        rejectDuplicateName(form, bindingResult, null);
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/ingredients");
            return "ingredients/form";
        }
        ingredientService.create(form);
        return "redirect:/ingredients";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("ingredient", ingredientService.get(id));
        return "ingredients/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Ingredient ingredient = ingredientService.get(id);
        IngredientForm form = new IngredientForm();
        form.setName(ingredient.getName());
        form.setUnit(ingredient.getUnit());
        model.addAttribute("ingredientForm", form);
        model.addAttribute("formAction", "/ingredients/" + id);
        model.addAttribute("ingredientId", id);
        return "ingredients/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("ingredientForm") IngredientForm form,
                         BindingResult bindingResult,
                         Model model) {
        rejectDuplicateName(form, bindingResult, id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/ingredients/" + id);
            model.addAttribute("ingredientId", id);
            return "ingredients/form";
        }
        ingredientService.update(id, form);
        return "redirect:/ingredients";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        ingredientService.delete(id);
        return "redirect:/ingredients";
    }

    private void rejectDuplicateName(IngredientForm form, BindingResult bindingResult, Long excludeId) {
        if (!bindingResult.hasFieldErrors("name") && ingredientService.nameExists(form.getName(), excludeId)) {
            bindingResult.rejectValue("name", "duplicate", "Ингредиент с таким названием уже есть");
        }
    }
}
