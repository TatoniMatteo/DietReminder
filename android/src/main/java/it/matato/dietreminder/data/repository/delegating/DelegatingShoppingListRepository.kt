package it.matato.dietreminder.data.repository.delegating

import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.data.repository.contracts.ShoppingListInitialItem
import it.matato.dietreminder.data.repository.contracts.ShoppingListReadRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import kotlinx.coroutines.flow.Flow

class DelegatingShoppingListRepository(
	private val onlineRepository: ShoppingListRepository,
	private val localCacheRepository: ShoppingListReadRepository,
	private val isOfflineProvider: () -> Boolean,
) : ShoppingListRepository {

	private val currentRepo: ShoppingListReadRepository
		get() = if (isOfflineProvider()) localCacheRepository else onlineRepository

	override val allShoppingLists: Flow<List<ShoppingListWithItems>> get() = currentRepo.allShoppingLists

	override fun observeShoppingList(id: Long): Flow<ShoppingListWithItems?> = currentRepo.observeShoppingList(id)

	override suspend fun createShoppingList(name: String, dietId: Long?, items: List<ShoppingListInitialItem>): Long {
		requireOnlineWrites(isOfflineProvider)
		return onlineRepository.createShoppingList(name, dietId, items)
	}

	override suspend fun updateShoppingListName(id: Long, name: String) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.updateShoppingListName(id, name)
	}

	override suspend fun deleteShoppingList(id: Long) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.deleteShoppingList(id)
	}

	override suspend fun addShoppingListItem(
		listId: Long, name: String, amount: String, unit: QuantityUnit, isCustom: Boolean): Long {
		requireOnlineWrites(isOfflineProvider)
		return onlineRepository.addShoppingListItem(listId, name, amount, unit, isCustom)
	}

	override suspend fun updateShoppingListItem(item: ShoppingListItem) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.updateShoppingListItem(item)
	}

	override suspend fun toggleShoppingListItemBought(itemId: Long, isBought: Boolean) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.toggleShoppingListItemBought(itemId, isBought)
	}

	override suspend fun toggleShoppingListItemDayBought(dayId: Long, isBought: Boolean) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.toggleShoppingListItemDayBought(dayId, isBought)
	}

	override suspend fun deleteShoppingListItem(itemId: Long) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.deleteShoppingListItem(itemId)
	}

	override suspend fun addDietIngredientsToShoppingList(
		listId: Long, dietId: Long, configs: List<DietImportItemConfig>) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.addDietIngredientsToShoppingList(listId, dietId, configs)
	}
}
