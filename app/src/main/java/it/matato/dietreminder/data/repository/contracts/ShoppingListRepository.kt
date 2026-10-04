package it.matato.dietreminder.data.repository.contracts

import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.data.model.QuantityUnit
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

data class DietImportItemConfig(
	val ingredientName: String,
	val isFresh: Boolean,
	val selectedDays: Set<DayOfWeek> = emptySet(),
)

data class ShoppingListInitialItem(
	val name: String,
	val amount: String = "",
	val unit: QuantityUnit = QuantityUnit.GRAMS,
)

interface ShoppingListReadRepository {
	val allShoppingLists: Flow<List<ShoppingListWithItems>>

	fun observeShoppingList(id: Long): Flow<ShoppingListWithItems?>
}

interface ShoppingListWriteRepository {
	suspend fun createShoppingList(
		name: String, dietId: Long? = null, items: List<ShoppingListInitialItem> = emptyList()): Long

	suspend fun updateShoppingListName(id: Long, name: String)
	suspend fun deleteShoppingList(id: Long)
	suspend fun addShoppingListItem(
		listId: Long, name: String, amount: String = "", unit: QuantityUnit = QuantityUnit.GRAMS,
		isCustom: Boolean = true): Long

	suspend fun updateShoppingListItem(item: ShoppingListItem)
	suspend fun toggleShoppingListItemBought(itemId: Long, isBought: Boolean)
	suspend fun toggleShoppingListItemDayBought(dayId: Long, isBought: Boolean)
	suspend fun deleteShoppingListItem(itemId: Long)
	suspend fun addDietIngredientsToShoppingList(listId: Long, dietId: Long, configs: List<DietImportItemConfig>)
}

interface ShoppingListRepository : ShoppingListReadRepository, ShoppingListWriteRepository
