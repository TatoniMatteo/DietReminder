package it.matato.dietreminder.domain

import it.matato.dietreminder.common.model.*
import org.junit.Assert.*
import org.junit.Test

class ShoppingPlanningTest {
    @Test fun categoriesArePredictableAndManualOverridesWin() {
        assertEquals("Frutta e verdura", FoodCategories.classify("  MELE bio "))
        assertEquals("Bevande", FoodCategories.classify("Latte di soia"))
        assertEquals("Altro", FoodCategories.classify("cioccolatte"))
        assertEquals("Mercato", FoodCategories.classify("Mele", "Mercato"))
    }
    @Test fun remindersFollowConsumptionDaysAndIgnoreAlreadyBoughtAllocations() {
        val snapshot = WorkspaceSnapshot(
            lists = listOf(CloudShoppingList(1,"Spesa",1)),
            items = listOf(CloudShoppingItem(1,1,"Insalata", isFresh=true)),
            days = listOf(CloudShoppingDay(1,1,1,"150",purchaseDayOfWeek=7), CloudShoppingDay(2,1,2,"200",isBought=true))
        )
        assertEquals(listOf("Insalata"), ShoppingPlanning.pendingTomorrow(snapshot,1,1))
        assertTrue(ShoppingPlanning.pendingTomorrow(snapshot,1,7).isEmpty())
        assertTrue(ShoppingPlanning.pendingTomorrow(snapshot,1,2).isEmpty())
        assertTrue(ShoppingPlanning.pendingTomorrow(snapshot,2,1).isEmpty())
    }
    @Test fun undatedPantryProductsUseTheAssociatedDietAndExplicitDaysTakePrecedence() {
        val snapshot = WorkspaceSnapshot(
            diets=listOf(CloudDiet(1,"uuid","Piano")),
            meals=listOf(CloudMeal(1,1,1,MealType.LUNCH,720)),
            courses=listOf(CloudCourse(1,1)), foods=listOf(CloudFood(1,1,"Riso","100")),
            lists=listOf(CloudShoppingList(1,"Spesa",1,1)),
            items=listOf(CloudShoppingItem(1,1,"riso"),CloudShoppingItem(2,1,"Mele",dayOfWeek=1),CloudShoppingItem(3,1,"Pane"),CloudShoppingItem(4,1,"Riso",isBought=true),CloudShoppingItem(5,1,"Riso",dayOfWeek=2))
        )
        assertEquals(listOf("riso","Mele"),ShoppingPlanning.pendingTomorrow(snapshot,1,1))
    }
}
