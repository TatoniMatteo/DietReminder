package it.matato.dietreminder.ui.screens.diets

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

fun dietsRobot(
    composeTestRule: SemanticsNodeInteractionsProvider,
    block: DietsScreenRobot.() -> Unit
) = DietsScreenRobot(composeTestRule).apply(block)

class DietsScreenRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

    fun clickAddDiet() {
        composeTestRule.onNode(hasContentDescription("New diet") or hasContentDescription("Nuova dieta"))
            .performClick()
    }

    fun typeDietName(name: String) {
        composeTestRule.onNode(hasText("Diet name") or hasText("Nome dieta") or hasText("Nome della dieta") or hasText(""))
            .performTextInput(name)
    }

    fun clickConfirmCreateDiet() {
        composeTestRule.onNode(hasText("Create") or hasText("Crea"))
            .performClick()
    }

    fun verifyDietExists(name: String) {
        composeTestRule.onNode(hasText(name), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    fun clickMoreOptionsForDiet(name: String) {
        // Clicchiamo sull'icona delle opzioni ("Configure") che si trova nello stesso elemento della dieta
        composeTestRule.onNode(hasContentDescription("Configure") or hasContentDescription("Configura"), useUnmergedTree = true)
            .performClick()
    }

    fun clickMenuOption(optionText: String) {
        // Mappatura delle traduzioni per rendere il test robusto ma selettivo
        val translatedText = when(optionText) {
            "Duplicate" -> "Duplica"
            "Delete" -> "Elimina"
            else -> optionText
        }
        composeTestRule.onNode(hasText(optionText) or hasText(translatedText))
            .performClick()
    }

    fun verifyDietCountHeader(count: Int) {
        composeTestRule.onNode(hasText(count.toString()), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
