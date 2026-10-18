package it.matato.dietreminder.data.repository.aggregate

import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.repository.contracts.ConfigRepository
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.data.repository.contracts.DietRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListInitialItem
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import kotlinx.coroutines.flow.Flow

class CompositeDietRepository(
	private val dietRepository: DietRepository,
	private val shoppingListRepository: ShoppingListRepository,
	private val configRepository: ConfigRepository,
) : DietRepository, ShoppingListRepository, ConfigRepository {

	override val all: Flow<List<Diet>> get() = dietRepository.all
	override val active: Flow<Diet?> get() = dietRepository.active
	override fun observeMeals(dietId: Long): Flow<List<MealWithDetails>> = dietRepository.observeMeals(dietId)
	override suspend fun getMeals(dietId: Long): List<MealWithDetails> = dietRepository.getMeals(dietId)
	override suspend fun create(name: String, window: Int): Long = dietRepository.create(name, window)
	override suspend fun updateDiet(diet: Diet) = dietRepository.updateDiet(diet)
	override suspend fun activate(id: Long) = dietRepository.activate(id)
	override suspend fun delete(id: Long): Int = dietRepository.delete(id)
	override suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>) =
		dietRepository.saveMeal(meal, mealCourses)

	override suspend fun deleteMeal(id: Long): Int = dietRepository.deleteMeal(id)
	override suspend fun duplicate(id: Long): Long = dietRepository.duplicate(id)
	override suspend fun exportJson(id: Long): String = dietRepository.exportJson(id)
	override fun parseDietJson(json: String): DietExport = dietRepository.parseDietJson(json)
	override suspend fun dietExists(uuid: String): Boolean = dietRepository.dietExists(uuid)
	override suspend fun importJson(json: String, overwrite: Boolean) =
		dietRepository.importJson(json, overwrite)

	override val allShoppingLists: Flow<List<ShoppingListWithItems>>
		get() = shoppingListRepository.allShoppingLists

	override fun observeShoppingList(id: Long): Flow<ShoppingListWithItems?> =
		shoppingListRepository.observeShoppingList(id)

	override suspend fun createShoppingList(
		name: String,
		dietId: Long?,
		items: List<ShoppingListInitialItem>,
	): Long = shoppingListRepository.createShoppingList(name, dietId, items)

	override suspend fun updateShoppingListName(id: Long, name: String) =
		shoppingListRepository.updateShoppingListName(id, name)

	override suspend fun deleteShoppingList(id: Long) = shoppingListRepository.deleteShoppingList(id)
	override suspend fun addShoppingListItem(
		listId: Long,
		name: String,
		amount: String,
		unit: QuantityUnit,
		isCustom: Boolean,
	): Long = shoppingListRepository.addShoppingListItem(listId, name, amount, unit, isCustom)

	override suspend fun updateShoppingListItem(item: ShoppingListItem) =
		shoppingListRepository.updateShoppingListItem(item)

	override suspend fun toggleShoppingListItemBought(itemId: Long, isBought: Boolean) =
		shoppingListRepository.toggleShoppingListItemBought(itemId, isBought)

	override suspend fun toggleShoppingListItemDayBought(dayId: Long, isBought: Boolean) =
		shoppingListRepository.toggleShoppingListItemDayBought(dayId, isBought)

	override suspend fun deleteShoppingListItem(itemId: Long) =
		shoppingListRepository.deleteShoppingListItem(itemId)

	override suspend fun addDietIngredientsToShoppingList(
		listId: Long,
		dietId: Long,
		configs: List<DietImportItemConfig>,
	) = shoppingListRepository.addDietIngredientsToShoppingList(listId, dietId, configs)

	override val defaultTimes: Flow<List<MealDefaultTime>> get() = configRepository.defaultTimes
	override suspend fun saveDefaultTime(type: MealType, timeMinutes: Int) =
		configRepository.saveDefaultTime(type, timeMinutes)

	override fun observeConfig(key: ConfigKey): Flow<AppConfig?> = configRepository.observeConfig(key)
	override suspend fun saveConfig(key: ConfigKey, value: String) =
		configRepository.saveConfig(key, value)

	override suspend fun resetDatabase() = configRepository.resetDatabase()
}
