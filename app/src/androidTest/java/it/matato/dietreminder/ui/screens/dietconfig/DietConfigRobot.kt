package it.matato.dietreminder.ui.screens.dietconfig

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick

fun dietConfigRobot(
    composeTestRule: SemanticsNodeInteractionsProvider,
    block: DietConfigRobot.() -> Unit
) = DietConfigRobot(composeTestRule).apply(block)

class DietConfigRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

    fun clickAddMeal() {
        composeTestRule.onNode(hasContentDescription("Aggiungi") or hasContentDescription("Add"))
            .performClick()
    }

    fun verifyMealInConfig(mealName: String) {
        composeTestRule.onNode(hasText(mealName), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    fun clickBack() {
        composeTestRule.onNode(hasContentDescription("Indietro") or hasContentDescription("Back"))
            .performClick()
    }
}
