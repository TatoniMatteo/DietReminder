package it.matato.dietreminder.ui.screens.shoppinglist

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.database.relation.*
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class ShoppingPurchasedSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun purchasesMoveToOneCollapsedSectionAndCanBeRestored() {
        val salad = ShoppingListItem(1, 1, "Insalata", category = "Verdura")
        val bread = ShoppingListItem(2, 1, "Pane", isBought = true, category = "Panetteria")
        val day = ShoppingListItemDay(1, 1, DayOfWeek.MONDAY)
        val state = mutableStateOf(ShoppingListWithItems(ShoppingList(1, "Spesa"), listOf(
            ShoppingListItemWithDays(salad, listOf(day)), ShoppingListItemWithDays(bread, emptyList()),
        )))
        compose.setContent {
            DietTheme {
                ShoppingDayPlanner(state.value, onToggleItem = {}, onToggleDay = { id, bought ->
                    state.value = state.value.copy(items = state.value.items.map { row ->
                        row.copy(days = row.days.map { if (it.id == id) it.copy(isBought = bought) else it })
                    })
                }, onEdit = {}, onUpdate = {}, onMove = { _, _, _ -> }, onDelete = {})
            }
        }
        compose.onNodeWithText("Insalata").assertIsDisplayed()
        compose.onNodeWithText("Pane").assertDoesNotExist()
        compose.onNodeWithTag("shoppingCheck_1:1").performClick()
        compose.onNodeWithText("Insalata").assertDoesNotExist()
        compose.onNodeWithText("Acquisti di Lunedì").assertDoesNotExist()
        compose.onNodeWithTag("shoppingPurchasedSection").performScrollTo().performClick()
        compose.onNodeWithText("Insalata").assertIsDisplayed()
        compose.onNodeWithText("Pane").assertIsDisplayed()
        compose.onNodeWithText("Panetteria", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("shoppingCheck_1:1").performClick()
        compose.onNodeWithText("Verdura · 1 da acquistare").assertIsDisplayed()
        compose.onNodeWithTag("shoppingCheck_1:1").assertIsOff()
    }
}
