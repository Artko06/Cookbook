package org.example.cookbook.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.example.cookbook.domain.Recipe;
import org.example.cookbook.dto.RecipeForm;
import org.example.cookbook.dto.RecipeIngredientForm;
import org.example.cookbook.dto.RecipeListItem;
import org.example.cookbook.dto.RecipeResponse;
import org.example.cookbook.exception.DuplicateResourceException;
import org.example.cookbook.service.RecipeService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeApiController {

    private final RecipeService recipeService;

    public RecipeApiController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public Page<RecipeListItem> list(@RequestParam(name = "q", required = false) String q,
                                     @RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "12") int size) {
        return recipeService.search(q, page, size);
    }

    @GetMapping("/my")
    public Page<RecipeListItem> my(@RequestParam(name = "page", defaultValue = "0") int page,
                                   @RequestParam(name = "size", defaultValue = "12") int size,
                                   Authentication authentication) {
        return recipeService.byAuthor(authentication.getName(), page, size);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable Long id) {
        return RecipeResponse.from(recipeService.get(id));
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeForm form,
                                                 Authentication authentication) {
        rejectDuplicateIngredients(form);
        Recipe created = recipeService.create(form, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/recipes/" + created.getId()))
                .body(RecipeResponse.from(created));
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id,
                                 @Valid @RequestBody RecipeForm form,
                                 Authentication authentication) {
        rejectDuplicateIngredients(form);
        return RecipeResponse.from(recipeService.update(id, form, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        recipeService.delete(id, authentication.getName());
    }

    private void rejectDuplicateIngredients(RecipeForm form) {
        if (form.getIngredients() == null) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (RecipeIngredientForm line : form.getIngredients()) {
            if (line == null || line.getIngredientId() == null) {
                continue;
            }
            if (!seen.add(line.getIngredientId())) {
                throw new DuplicateResourceException("Ингредиент не должен повторяться в составе");
            }
        }
    }
}
