package it.matato.dietreminder.common.model

import org.junit.Assert.*
import org.junit.Test
class ShoppingRulesTest {
    @Test fun automaticCategoriesHandleAccentsPhrasesBoundariesAndManualOverrides() {
        for ((name,category) in listOf("Mele" to "Frutta e verdura","Latte" to "Latte, uova e formaggi","Verdure surgelate" to "Surgelati","Oggetto senza categoria" to "Altro")) assertEquals(name,category,FoodCategories.classify(name))
        assertEquals("Il mio reparto",FoodCategories.classify("Mele"," Il mio reparto "))
        assertEquals(FoodCategories.classify("Mele"),FoodCategories.classify("Mele","  "))
        assertEquals("caffe",FoodCategories.normalize(" Caffè "))
        assertEquals("Altro",FoodCategories.classify("melencolico"))
    }
    @Test fun remindersUseConsumptionDaysAndNeverCombineAccountsListsOrBoughtAllocations() {
        val snapshot=WorkspaceSnapshot(
            lists=listOf(CloudShoppingList(1,"Spesa",1,null)),
            items=listOf(CloudShoppingItem(1,1,"Mele","300",isBought=true,isFresh=true),CloudShoppingItem(2,2,"Altra lista","1"),CloudShoppingItem(3,1,"Pane","100",dayOfWeek=1)),
            days=listOf(CloudShoppingDay(1,1,1,"150",isBought=false,purchaseDayOfWeek=7),CloudShoppingDay(2,1,2,"150",isBought=true)))
        assertEquals(listOf("Mele","Pane"),ShoppingPlanning.pendingTomorrow(snapshot,1,1))
        assertTrue(ShoppingPlanning.pendingTomorrow(snapshot,1,2).isEmpty())
        assertTrue(ShoppingPlanning.pendingTomorrow(snapshot,999,1).isEmpty())
    }
}
