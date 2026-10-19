package it.matato.dietreminder.widget

import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.database.relation.*
import it.matato.dietreminder.data.model.MealType
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class WidgetModelsTest {
    private val now = LocalDateTime.of(2026, 10, 19, 11, 0)
    private val epoch = 1_000_000L
    private fun meal(id: Long, minutes: Int, day: DayOfWeek = DayOfWeek.MONDAY) = MealWithDetails(Meal(id, 1, day, MealType.LUNCH, minutes), emptyList())
    private val meals = listOf(meal(3, 1200), meal(1, 480), meal(4, 480, DayOfWeek.TUESDAY), meal(2, 780))
    private fun selection(id: Long = 1, scope: String = "local", at: Long = epoch, date: String = now.toLocalDate().toString()) = MealWidgetSelection(id, scope, at, date)
    private fun model(selected: MealWidgetSelection? = null) = mealWidgetModel(meals, now, 90, "local", selected, epoch)
    @Test fun defaultsToNextMealAndSortsTodayChronologically() {
        assertEquals(2L, model().selected!!.meal.meal.id)
        assertFalse(model().browsing)
        assertEquals(listOf(1L,2L,3L), model().today.map { it.meal.id })
    }
    @Test fun navigatesBothDirectionsAndWrapsWithinToday() {
        assertEquals(1L, model().adjacent(-1)!!.meal.id)
        assertEquals(3L, model().adjacent(1)!!.meal.id)
        assertEquals(3L, model(selection()).adjacent(-1)!!.meal.id)
        assertEquals(1L, model(selection(3)).adjacent(1)!!.meal.id)
    }
    @Test fun manualSelectionCanShowAnEarlierMeal() {
        val model = model(selection())
        assertTrue(model.browsing)
        assertEquals(1L, model.selected!!.meal.meal.id)
    }
    @Test fun expiredFutureOtherAccountOtherDayAndDeletedSelectionsReturnToNext() {
        for (selection in listOf(selection(at=epoch-900_000), selection(at=epoch+1), selection(scope="other"), selection(date="2026-10-18"), selection(id=999), selection(id=4))) {
            assertEquals(2L, model(selection).selected!!.meal.meal.id)
            assertFalse(model(selection).browsing)
        }
    }
    @Test fun afterLastMealDefaultsToTomorrowButCanBrowseToday() {
        val model = mealWidgetModel(meals, now.withHour(23), 0, "local")
        assertEquals(4L, model.selected!!.meal.meal.id)
        assertEquals(1L, model.adjacent(1)!!.meal.id)
        assertEquals(3L, model.adjacent(-1)!!.meal.id)
    }
    @Test fun emptyAndSingleMealDaysAreSafe() {
        val empty = mealWidgetModel(emptyList(), now, 90, "local")
        assertNull(empty.selected); assertNull(empty.adjacent(1)); assertNull(empty.adjacent(-1))
        assertEquals(2L, mealWidgetModel(listOf(meal(2,780)), now, 0, "local").adjacent(1)!!.meal.id)
    }
    @Test fun equalTimesHaveDeterministicNavigation() {
        val model = mealWidgetModel(listOf(meal(2,780),meal(1,780)),now,0,"local")
        assertEquals(listOf(1L,2L),model.today.map { it.meal.id })
    }
    private val parent = ShoppingListItem(id=1, shoppingListId=1, name="Mele", amount="300", isFresh=true, isBought=true)
    private fun list(id: Long, dietId: Long? = null, bought: Boolean = false) = ShoppingListWithItems(ShoppingList(id=id,name="Lista $id",dietId=dietId), listOf(ShoppingListItemWithDays(parent.copy(shoppingListId=id,isBought=bought),emptyList())))
    @Test fun dailyAllocationsKeepOwnQuantityDayAndBoughtState() {
        val list = ShoppingListWithItems(ShoppingList(id=1,name="Spesa"), listOf(ShoppingListItemWithDays(parent,listOf(
            ShoppingListItemDay(id=11,shoppingListItemId=1,dayOfWeek=DayOfWeek.MONDAY,amount="150",isBought=false,purchaseDayOfWeek=DayOfWeek.SUNDAY),
            ShoppingListItemDay(id=12,shoppingListItemId=1,dayOfWeek=DayOfWeek.TUESDAY,amount="200",isBought=true)))))
        val rows=shoppingWidgetEntries(list)
        assertEquals(2,rows.size); assertEquals(listOf("1:11","1:12"),rows.map { it.key })
        assertEquals(listOf(false,true),rows.map { it.bought }); assertEquals(listOf(7,2),rows.map { it.purchaseDay })
        assertEquals(listOf("150 g","200 g"),rows.map { it.quantity }); assertEquals("Frutta e verdura",rows[0].category)
    }
    @Test fun pantryRowsAndManualCategoriesAreRetained() {
        val list=list(1).copy(items=listOf(ShoppingListItemWithDays(parent.copy(category="Il mio negozio",isBought=false),emptyList())))
        val row=shoppingWidgetEntries(list).single()
        assertNull(row.dayId); assertNull(row.purchaseDay); assertEquals("Il mio negozio",row.category)
    }
    @Test fun selectionSurvivesRefreshAndDeletionFallsBackToActivePendingList() {
        val lists=listOf(list(1,bought=true),list(2,9),list(3,7))
        assertEquals(1L,selectWidgetList(lists,1,7)!!.list.id)
        assertEquals(3L,selectWidgetList(lists,999,7)!!.list.id)
        assertEquals(2L,selectWidgetList(lists,null,99)!!.list.id)
        assertEquals(1L,selectWidgetList(listOf(list(1,bought=true)),null,null)!!.list.id)
        assertNull(selectWidgetList(emptyList(),null,null))
    }
}
