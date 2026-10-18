package it.matato.dietreminder.ui.screens.shoppinglist

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

fun shoppingListsRobot(
	composeTestRule: SemanticsNodeInteractionsProvider,
	block: ShoppingListsRobot.() -> Unit,
) = ShoppingListsRobot(composeTestRule).apply(block)

class ShoppingListsRobot(private val composeTestRule: SemanticsNodeInteractionsProvider) {

	fun verifyTitle() {
		composeTestRule.onNode(hasText("Liste della spesa") or hasText("Shopping lists"))
			.assertExists()
	}

	fun clickCreateNewList() {
		composeTestRule.onNode(hasContentDescription("Nuova lista della spesa") or hasContentDescription("New shopping list"))
			.performClick()
	}

	fun confirmCreateList() {
		composeTestRule.onNode(hasText("Crea") or hasText("Create"))
			.performClick()
	}

	fun selectList(listName: String) {
		composeTestRule.onNode(hasText(listName))
			.performClick()
	}

	fun clickAddItemFab() {
		composeTestRule.onNode(hasContentDescription("Aggiungi elemento") or hasContentDescription("Add item"))
			.performClick()
	}

	fun clickAddManualItem() {
		composeTestRule.onNode(hasText("Aggiungi elemento manuale") or hasText("Add manual item"))
			.performClick()
	}

	fun typeItemName(name: String) {
		composeTestRule.onNode(hasText("Nome alimento") or hasText("Item name"))
			.performTextInput(name)
	}

	fun clickSaveDialog() {
		composeTestRule.onNode(hasText("Salva") or hasText("Save"))
			.performClick()
	}

	fun verifyItemDisplayed(itemName: String) {
		composeTestRule.onNode(hasText(itemName))
			.assertExists()
	}

	fun toggleItemBought(itemName: String) {
		composeTestRule.onNode(hasText(itemName))
			.performClick()
	}

	fun verifyCompletionCardDisplayed() {
		composeTestRule.onNode(hasText("Tutto acquistato! 🎉") or hasText("All items purchased! 🎉"))
			.assertExists()
	}
}
