package it.matato.dietreminder.ui.screens.week

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick

fun weekRobot(
    composeTestRule: SemanticsNodeInteractionsProvider,
    block: WeekScreenRobot.() -> Unit
) = WeekScreenRobot(composeTestRule).apply(block)

class WeekScreenRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

    fun verifyEmptyState() {
        composeTestRule.onNode(hasText("Nessuna dieta attiva") or hasText("No active diet"))
            .assertIsDisplayed()
    }

    fun selectDay(dayName: String) {
        // dayName può essere "L", "M", "M", "G", "V", "S", "D"
        composeTestRule.onNode(hasText(dayName))
            .performClick()
    }

    fun verifyMealVisible(mealName: String) {
        composeTestRule.onNode(hasText(mealName), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
