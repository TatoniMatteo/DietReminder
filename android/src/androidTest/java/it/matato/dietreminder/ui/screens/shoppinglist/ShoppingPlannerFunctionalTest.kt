package it.matato.dietreminder.ui.screens.shoppinglist

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.database.relation.*
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class ShoppingPlannerFunctionalTest {
    @get:Rule val compose=createComposeRule()
    private val item=ShoppingListItem(id=1,shoppingListId=1,name="Insalata",isFresh=true)
    private val day=ShoppingListItemDay(id=1,shoppingListItemId=1,dayOfWeek=DayOfWeek.MONDAY,amount="150")
    private val list=ShoppingListWithItems(ShoppingList(id=1,name="Spesa"),listOf(ShoppingListItemWithDays(item,listOf(day))))
    @Test fun groupsByDayAndCategoryAndFiltersEmptyDays() {
        compose.setContent { DietTheme { ShoppingListDetailContent(listWithItems = list, hasActiveDiet = false, onBack = {}, onToggleItem = {}, onToggleDay = {_,_->}, onDeleteItem = {}, onUpdateItem = {}, onAddCustomClick = {}, onImportDietClick = {}, onDeleteList = {}, onRenameList = {}) } }
        compose.onNodeWithText("Acquisti di Lunedì").assertIsDisplayed()
        compose.onNodeWithText("Frutta e verdura · 1 da acquistare").assertIsDisplayed()
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        val target = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "shopping-planner-preview.png")
        target.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithTag("shoppingDay_2").performClick()
        compose.onNodeWithText("Insalata").assertDoesNotExist()
        compose.onNodeWithTag("shoppingDay_1").performClick()
        compose.onNodeWithText("Insalata").assertIsDisplayed()
    }
    @Test fun movesPurchaseWithoutMovingConsumptionAndStoresManualCategory() {
        var updatedItem: ShoppingListItem?=null
        var updatedDay: ShoppingListItemDay?=null
        compose.setContent { DietTheme { ShoppingDayPlanner(list,{}, {_,_->}, {}, {updatedItem=it}, {_,_,_->}, {}, onAllocation={updatedDay=it}) } }
        compose.onNodeWithTag("shoppingOptions_1:1").performClick()
        compose.onNodeWithText("Giorno e categoria").performClick()
        compose.onNodeWithText("Categoria personalizzata").performTextInput("Orto")
        compose.onNodeWithText("Come il giorno di utilizzo").performScrollTo().performClick()
        compose.onNodeWithTag("shoppingChoice_Giorno d’acquisto_Domenica").performScrollTo().performClick()
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("Orto",updatedItem?.category)
            assertEquals(DayOfWeek.SUNDAY,updatedDay?.purchaseDayOfWeek)
            assertEquals(DayOfWeek.MONDAY,updatedDay?.dayOfWeek)
            assertEquals("150",updatedDay?.amount)
        }
    }
    @Test fun persistentReminderCheckIsRegisteredAtApplicationStartup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val work = androidx.work.WorkManager.getInstance(context).getWorkInfosForUniqueWork("shopping-reminders").get(10, java.util.concurrent.TimeUnit.SECONDS)
        assertTrue(work.any { !it.state.isFinished })
    }

}
