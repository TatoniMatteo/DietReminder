package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput

fun mealDetailRobot(
	composeTestRule: SemanticsNodeInteractionsProvider,
	block: MealDetailRobot.() -> Unit,
) = MealDetailRobot(composeTestRule).apply(block)

class MealDetailRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

	private fun scrollTo(matcher: SemanticsMatcher) {
		composeTestRule.onNodeWithTag("meal_detail_list").performScrollToNode(matcher)
	}

	fun typeDescription(text: String) {
		composeTestRule.onNode(hasText("Note") or hasText("Notes"))
			.performTextInput(text)
	}

	fun selectMealType(typeText: String) {
		composeTestRule.onNode(hasText(typeText))
			.performClick()
	}

	fun clickAddCourse() {
		scrollTo(hasText("Aggiungi portata") or hasText("Add course"))
		composeTestRule.onNode(hasText("Aggiungi portata") or hasText("Add course"))
			.performClick()
	}

	fun clickAddFoodItem() {
		scrollTo(hasText("Aggiungi alimento") or hasText("Add food item"))
		composeTestRule.onNode(hasText("Aggiungi alimento") or hasText("Add food item"))
			.performClick()
	}

	fun typeFoodItemName(name: String) {
		scrollTo(hasText("Alimento") or hasText("Food item"))
		composeTestRule.onNode(hasText("Alimento") or hasText("Food item"))
			.performTextInput(name)
	}

	fun typeQuantityAmount(amount: String) {
		scrollTo(hasText("Quantità") or hasText("Quantity"))
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
