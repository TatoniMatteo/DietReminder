package it.matato.dietreminder.widget

import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListInitialItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ShoppingWidgetActionsTest {
    @Test fun purchaseAndUndoReachTheSharedRepositoryAndRepeatedActionsAreIdempotent() = runBlocking {
        val repo=FakeDietRepository();val list=repo.createShoppingList("Spesa",items=listOf(ShoppingListInitialItem("Mele")))
        val item=repo.observeShoppingList(list).first()!!.items.single().item.id
        assertTrue(applyWidgetPurchase(repo,list,item,null,true,{true},{false}))
        assertTrue(applyWidgetPurchase(repo,list,item,null,true,{true},{false}))
        assertTrue(repo.observeShoppingList(list).first()!!.items.single().item.isBought)
        assertTrue(applyWidgetPurchase(repo,list,item,null,false,{true},{false}))
        assertFalse(repo.observeShoppingList(list).first()!!.items.single().item.isBought)
    }
    @Test fun staleAccountOfflineWritesMissingListAndMissingItemNeverMutateAnything() = runBlocking {
        val repo=FakeDietRepository();val list=repo.createShoppingList("Spesa",items=listOf(ShoppingListInitialItem("Mele")))
        val item=repo.observeShoppingList(list).first()!!.items.single().item.id
        assertFalse(applyWidgetPurchase(repo,list,item,null,true,{false},{false}))
        assertFalse(applyWidgetPurchase(repo,list,item,null,true,{true},{true}))
        assertFalse(applyWidgetPurchase(repo,999,item,null,true,{true},{false}))
        assertFalse(applyWidgetPurchase(repo,list,999,null,true,{true},{false}))
        assertFalse(applyWidgetPurchase(repo,list,item,999,true,{true},{false}))
        assertFalse(repo.observeShoppingList(list).first()!!.items.single().item.isBought)
    }
    @Test fun accountSwitchWhileReadingDoesNotWriteToEitherAccount() = runBlocking {
        val backing=FakeDietRepository();val list=backing.createShoppingList("Spesa",items=listOf(ShoppingListInitialItem("Mele")))
        val item=backing.observeShoppingList(list).first()!!.items.single().item.id
        var current=true
        val repo=object: ShoppingListRepository by backing {
            override fun observeShoppingList(id: Long)=flow { current=false;emit(backing.observeShoppingList(id).first()) }
        }
        assertFalse(applyWidgetPurchase(repo,list,item,null,true,{current},{false}))
        assertFalse(backing.observeShoppingList(list).first()!!.items.single().item.isBought)
    }
}
