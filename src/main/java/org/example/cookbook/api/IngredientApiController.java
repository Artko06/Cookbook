package org.example.cookbook.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.example.cookbook.domain.Ingredient;
import org.example.cookbook.dto.IngredientForm;
import org.example.cookbook.dto.IngredientResponse;
import org.example.cookbook.exception.DuplicateResourceException;
import org.example.cookbook.service.IngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/ingredients")
public class IngredientApiController {

    private final IngredientService ingredientService;

    public IngredientApiController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public List<IngredientResponse> list(@RequestParam(name = "q", required = false) String q) {
        return ingredientService.findAll(q).stream().map(IngredientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public IngredientResponse get(@PathVariable Long id) {
        return IngredientResponse.from(ingredientService.get(id));
    }

    @PostMapping
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientForm form) {
        rejectDuplicateName(form.getName(), null);
        Ingredient created = ingredientService.create(form);
        return ResponseEntity.created(URI.create("/api/v1/ingredients/" + created.getId()))
                .body(IngredientResponse.from(created));
    }

    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable Long id, @Valid @RequestBody IngredientForm form) {
        rejectDuplicateName(form.getName(), id);
        return IngredientResponse.from(ingredientService.update(id, form));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ingredientService.delete(id);
    }

    private void rejectDuplicateName(String name, Long excludeId) {
        if (ingredientService.nameExists(name, excludeId)) {
            throw new DuplicateResourceException("Ингредиент с таким названием уже есть");
        }
    }
}
