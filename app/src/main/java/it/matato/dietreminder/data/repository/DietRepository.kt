package it.matato.dietreminder.data.repository

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

interface DietRepository {
	val all: Flow<List<Diet>>
	val active: Flow<Diet?>
	val defaultTimes: Flow<List<MealDefaultTime>>
	val allShoppingLists: Flow<List<ShoppingListWithItems>>

	fun observeShoppingList(id: Long): Flow<ShoppingListWithItems?>
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

	suspend fun saveDefaultTime(type: MealType, timeMinutes: Int)
	fun observeConfig(key: ConfigKey): Flow<AppConfig?>
	suspend fun saveConfig(key: ConfigKey, value: String)
	fun observeMeals(dietId: Long): Flow<List<MealWithDetails>>
	suspend fun getMeals(dietId: Long): List<MealWithDetails>
	suspend fun create(name: String, window: Int): Long
	suspend fun updateDiet(diet: Diet)
	suspend fun activate(id: Long)
	suspend fun delete(id: Long): Int
	suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>)
	suspend fun deleteMeal(id: Long): Int
	suspend fun resetDatabase()
	suspend fun duplicate(id: Long): Long
	suspend fun exportJson(id: Long): String
	fun parseDietJson(json: String): DietExport
	suspend fun dietExists(uuid: String): Boolean
	suspend fun importJson(json: String, overwrite: Boolean = false)
}
