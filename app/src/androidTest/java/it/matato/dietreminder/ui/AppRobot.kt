package it.matato.dietreminder.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import it.matato.dietreminder.ui.screens.diets.DietsScreenRobot
import it.matato.dietreminder.ui.screens.diets.dietsRobot

fun appRobot(
    composeTestRule: SemanticsNodeInteractionsProvider,
    block: AppRobot.() -> Unit
) = AppRobot(composeTestRule).apply(block)

class AppRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

    fun navigateToWeek() {
        composeTestRule.onNode(hasText("Settimana") or hasText("Week"))
            .performClick()
    }

    fun navigateToDiets() {
        composeTestRule.onNode(hasText("Diete") or hasText("Diets"))
            .performClick()
    }

    fun navigateToHydration() {
        composeTestRule.onNode(hasText("Idratazione") or hasText("Hydration"))
            .performClick()
    }

    fun navigateToSettings() {
        composeTestRule.onNode(hasText("Impostazioni") or hasText("Settings"))
            .performClick()
    }

    // Helper per saltare al robot specifico della schermata
    fun onDietsScreen(block: DietsScreenRobot.() -> Unit) {
        dietsRobot(composeTestRule, block)
    }
}
