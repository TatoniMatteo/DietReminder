package it.matato.dietreminder.data.auth

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
import kotlinx.coroutines.flow.flatMapLatest

@javax.inject.Singleton
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScopedAccountRepository @javax.inject.Inject constructor(private val accounts: AccountRepositories) : DietRepository, ShoppingListRepository, ConfigRepository {

    fun snapshot() = accounts.current.let { repositories ->
        it.matato.dietreminder.data.repository.aggregate.CompositeDietRepository(repositories.diets, repositories.shopping, repositories.config)
    }

	override val all: Flow<List<Diet>> get() = accounts.changes.flatMapLatest { it.diets.all }
	override val active: Flow<Diet?> get() = accounts.changes.flatMapLatest { it.diets.active }
	override fun observeMeals(dietId: Long): Flow<List<MealWithDetails>> = accounts.changes.flatMapLatest { it.diets.observeMeals(dietId) }
	override suspend fun getMeals(dietId: Long): List<MealWithDetails> = accounts.current.diets.getMeals(dietId)
	override suspend fun create(name: String, window: Int): Long = accounts.current.diets.create(name, window)
	override suspend fun updateDiet(diet: Diet) = accounts.current.diets.updateDiet(diet)
	override suspend fun activate(id: Long) = accounts.current.diets.activate(id)
	override suspend fun delete(id: Long): Int = accounts.current.diets.delete(id)
	override suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>) =
		accounts.current.diets.saveMeal(meal, mealCourses)

	override suspend fun deleteMeal(id: Long): Int = accounts.current.diets.deleteMeal(id)
	override suspend fun duplicate(id: Long): Long = accounts.current.diets.duplicate(id)
	override suspend fun exportJson(id: Long): String = accounts.current.diets.exportJson(id)
	override fun parseDietJson(json: String): DietExport = accounts.current.diets.parseDietJson(json)
	override suspend fun dietExists(uuid: String): Boolean = accounts.current.diets.dietExists(uuid)
	override suspend fun importJson(json: String, overwrite: Boolean) =
		accounts.current.diets.importJson(json, overwrite)

    override suspend fun updateShoppingAllocation(day: it.matato.dietreminder.data.database.entity.ShoppingListItemDay) { accounts.current.shopping.updateShoppingAllocation(day) }
    override suspend fun updateShoppingReminders(listId: Long, enabled: Boolean, timeMinutes: Int) { accounts.current.shopping.updateShoppingReminders(listId, enabled, timeMinutes) }

    override suspend fun moveShoppingPurchase(itemId: Long, dayId: Long?, purchaseDay: java.time.DayOfWeek?) { accounts.current.shopping.moveShoppingPurchase(itemId, dayId, purchaseDay) }

	override val allShoppingLists: Flow<List<ShoppingListWithItems>>
		get() = accounts.changes.flatMapLatest { it.shopping.allShoppingLists }

	override fun observeShoppingList(id: Long): Flow<ShoppingListWithItems?> =
		accounts.changes.flatMapLatest { it.shopping.observeShoppingList(id) }

	override suspend fun createShoppingList(
		name: String,
		dietId: Long?,
		items: List<ShoppingListInitialItem>,
	): Long = accounts.current.shopping.createShoppingList(name, dietId, items)

	override suspend fun updateShoppingListName(id: Long, name: String) =
		accounts.current.shopping.updateShoppingListName(id, name)

	override suspend fun deleteShoppingList(id: Long) = accounts.current.shopping.deleteShoppingList(id)
	override suspend fun addShoppingListItem(
		listId: Long,
		name: String,
		amount: String,
		unit: QuantityUnit,
		isCustom: Boolean,
	): Long = accounts.current.shopping.addShoppingListItem(listId, name, amount, unit, isCustom)

	override suspend fun updateShoppingListItem(item: ShoppingListItem) =
		accounts.current.shopping.updateShoppingListItem(item)

	override suspend fun toggleShoppingListItemBought(itemId: Long, isBought: Boolean) =
		accounts.current.shopping.toggleShoppingListItemBought(itemId, isBought)

	override suspend fun toggleShoppingListItemDayBought(dayId: Long, isBought: Boolean) =
		accounts.current.shopping.toggleShoppingListItemDayBought(dayId, isBought)

	override suspend fun deleteShoppingListItem(itemId: Long) =
		accounts.current.shopping.deleteShoppingListItem(itemId)

	override suspend fun addDietIngredientsToShoppingList(
		listId: Long,
		dietId: Long,
		configs: List<DietImportItemConfig>,
	) = accounts.current.shopping.addDietIngredientsToShoppingList(listId, dietId, configs)

	override val defaultTimes: Flow<List<MealDefaultTime>> get() = accounts.changes.flatMapLatest { it.config.defaultTimes }
	override suspend fun saveDefaultTime(type: MealType, timeMinutes: Int) =
		accounts.current.config.saveDefaultTime(type, timeMinutes)

	override fun observeConfig(key: ConfigKey): Flow<AppConfig?> = accounts.changes.flatMapLatest { it.config.observeConfig(key) }
	override suspend fun saveConfig(key: ConfigKey, value: String) =
		accounts.current.config.saveConfig(key, value)

	override suspend fun resetDatabase() = accounts.current.config.resetDatabase()
}
