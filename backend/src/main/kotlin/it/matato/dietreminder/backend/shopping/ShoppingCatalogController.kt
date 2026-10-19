package it.matato.dietreminder.backend.shopping

import it.matato.dietreminder.common.model.FoodCategories
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.tags.Tag

@RestController
@Tag(name = "Shopping", description = "Categorie condivise per l’organizzazione della spesa")
class ShoppingCatalogController {
    @GetMapping("/api/app/shopping-categories")
    fun categories() = FoodCategories.all
}
