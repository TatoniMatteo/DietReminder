package it.matato.dietreminder.ui.screens.ingredients

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick

fun ingredientsRobot(
    composeTestRule: SemanticsNodeInteractionsProvider,
    block: IngredientsScreenRobot.() -> Unit
) = IngredientsScreenRobot(composeTestRule).apply(block)

class IngredientsScreenRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

    fun verifyTitle() {
        composeTestRule.onNode(hasText("Lista ingredienti") or hasText("Ingredients list"), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    fun verifyIngredientDisplayed(name: String) {
        composeTestRule.onNode(hasText(name), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    fun clickIngredient(name: String) {
        composeTestRule.onNode(hasText(name), useUnmergedTree = true)
            .performClick()
    }

    fun verifyQuantityDisplayed(quantity: String) {
        composeTestRule.onNode(hasText(quantity), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
