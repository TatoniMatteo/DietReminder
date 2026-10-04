package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

fun mealDetailRobot(
	composeTestRule: SemanticsNodeInteractionsProvider,
	block: MealDetailRobot.() -> Unit,
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

	fun clickAddCourse() {
		composeTestRule.onNode(hasText("Aggiungi portata") or hasText("Add course"))
			.performClick()
	}

	fun clickAddFoodItem() {
		composeTestRule.onNode(hasText("Aggiungi alimento") or hasText("Add food item"))
			.performClick()
	}

	fun typeFoodItemName(name: String) {
		composeTestRule.onNode(hasText("Alimento") or hasText("Food item"))
			.performTextInput(name)
	}

	fun typeQuantityAmount(amount: String) {
		composeTestRule.onNode(hasText("Quantità") or hasText("Quantity"))
			.performTextInput(amount)
	}

	fun clickSave() {
		composeTestRule.onNode(
			hasText("Salva") or hasText("Save") or hasContentDescription("Salva") or hasContentDescription(
				"Save"))
			.performClick()
	}

	fun verifyFoodItemDisplayed(name: String) {
		composeTestRule.onNode(hasText(name))
			.assertExists()
	}
}
