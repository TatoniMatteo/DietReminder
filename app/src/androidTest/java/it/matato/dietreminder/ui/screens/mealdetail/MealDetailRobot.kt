package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

fun mealDetailRobot(
    composeTestRule: SemanticsNodeInteractionsProvider,
    block: MealDetailRobot.() -> Unit
) = MealDetailRobot(composeTestRule).apply(block)

class MealDetailRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

    fun typeDescription(text: String) {
        composeTestRule.onNode(hasText("Note") or hasText("Notes"))
            .performTextInput(text)
    }

    fun selectMealType(typeText: String) {
        composeTestRule.onNode(hasText(typeText))
            .performClick()
    }

    fun clickSave() {
        composeTestRule.onNode(hasText("Salva") or hasText("Save") or hasContentDescription("Salva") or hasContentDescription("Save"))
            .performClick()
    }
}
