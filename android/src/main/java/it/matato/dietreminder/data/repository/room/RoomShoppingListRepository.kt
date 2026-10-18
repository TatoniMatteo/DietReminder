package it.matato.dietreminder.data.repository.room

import androidx.room.withTransaction
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.dao.DietDao
import it.matato.dietreminder.data.database.dao.MealDao
import it.matato.dietreminder.data.database.dao.ShoppingListDao
import it.matato.dietreminder.data.database.entity.ShoppingList
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.entity.ShoppingListItemDay
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.data.repository.contracts.ShoppingListInitialItem
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import it.matato.dietreminder.domain.QuantityAggregator
import it.matato.dietreminder.domain.toIngredientSummaries
import kotlinx.coroutines.flow.Flow

class RoomShoppingListRepository(
	private val database: AppDatabase,
	private val shoppingLists: ShoppingListDao,
	private val diets: DietDao,
	private val meals: MealDao,
) : ShoppingListRepository {

	override val allShoppingLists: Flow<List<ShoppingListWithItems>> = shoppingLists.observeAllWithItems()

	override fun observeShoppingList(id: Long): Flow<ShoppingListWithItems?> =
		shoppingLists.observeWithItems(id)

	override suspend fun createShoppingList(
		name: String,
		dietId: Long?,
		items: List<ShoppingListInitialItem>,
	): Long = database.withTransaction {
		val listId = shoppingLists.insertList(
			ShoppingList(
				name = name,
				dietId = dietId,
			),
		)

		if (items.isNotEmpty()) {
			val listItems = items.map { initial ->
				ShoppingListItem(
					shoppingListId = listId,
					name = initial.name,
					amount = initial.amount,
					unit = initial.unit,
					isBought = false,
					isCustom = true,
				)
			}
			shoppingLists.insertItems(listItems)
		}

		listId
	}

	override suspend fun updateShoppingListName(id: Long, name: String) {
		val current = shoppingLists.getWithItems(id)?.list ?: return
		shoppingLists.updateList(current.copy(name = name))
	}

	override suspend fun deleteShoppingList(id: Long) {
		shoppingLists.deleteList(id)
	}

	override suspend fun addShoppingListItem(
		listId: Long,
		name: String,
		amount: String,
		unit: QuantityUnit,
		isCustom: Boolean,
	): Long {
		return shoppingLists.insertItem(
			ShoppingListItem(
				shoppingListId = listId,
				name = name,
				amount = amount,
				unit = unit,
				isBought = false,
				isCustom = isCustom,
			),
		)
	}

	override suspend fun updateShoppingListItem(item: ShoppingListItem) {
		shoppingLists.updateItem(item)
	}

	override suspend fun toggleShoppingListItemBought(itemId: Long, isBought: Boolean) {
		shoppingLists.updateBoughtStatus(itemId, isBought)
	}

	override suspend fun toggleShoppingListItemDayBought(dayId: Long, isBought: Boolean) = database.withTransaction {
		shoppingLists.updateDayBoughtStatus(dayId, isBought)
		val itemId = shoppingLists.getItemIdForDay(dayId) ?: return@withTransaction
		val days = shoppingLists.getItemDays(itemId)
		val allBought = days.isNotEmpty() && days.all { it.isBought }
		shoppingLists.updateBoughtStatus(itemId, allBought)
	}

	override suspend fun deleteShoppingListItem(itemId: Long) {
		shoppingLists.deleteItem(itemId)
	}

	override suspend fun addDietIngredientsToShoppingList(
		listId: Long,
		dietId: Long,
		configs: List<DietImportItemConfig>,
	) = database.withTransaction {
		val mealDetails = meals.getAllWithDetails(dietId)
		val summaries = mealDetails.toIngredientSummaries()
		val configMap = configs.associateBy { it.ingredientName }

		summaries.forEach { summary ->
			val config = configMap[summary.name] ?: return@forEach

			if (config.isFresh) {
				val byDay = summary.occurrences.groupBy { it.dayOfWeek }
				val targetDays = if (config.selectedDays.isNotEmpty()) {
					byDay.filterKeys { it in config.selectedDays }
				} else {
					byDay
				}

				if (targetDays.isNotEmpty()) {
					val itemId = shoppingLists.insertItem(
						ShoppingListItem(
							shoppingListId = listId,
							name = summary.name,
							isBought = false,
							isCustom = false,
							isFresh = true,
						),
					)

					val dayEntities = targetDays.map { (dayOfWeek, occurrences) ->
						val dayAgg = QuantityAggregator.aggregateOccurrences(occurrences)
						ShoppingListItemDay(
							shoppingListItemId = itemId,
							dayOfWeek = dayOfWeek,
							amount = dayAgg.amount,
							unit = dayAgg.unit,
							isBought = false,
						)
					}

					shoppingLists.insertItemDays(dayEntities)
				}
			} else {
				val totalAgg = QuantityAggregator.aggregateOccurrences(summary.occurrences)
				shoppingLists.insertItem(
					ShoppingListItem(
						shoppingListId = listId,
						name = summary.name,
						amount = totalAgg.amount,
						unit = totalAgg.unit,
						isBought = false,
						isCustom = false,
						isFresh = false,
					),
				)
			}
		}
	}
}
