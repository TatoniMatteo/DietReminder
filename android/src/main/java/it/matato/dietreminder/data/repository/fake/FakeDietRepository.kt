package it.matato.dietreminder.data.repository.fake

import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.database.entity.ShoppingList
import it.matato.dietreminder.data.database.entity.ShoppingListItem
import it.matato.dietreminder.data.database.entity.ShoppingListItemDay
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.database.relation.ShoppingListItemWithDays
import it.matato.dietreminder.data.database.relation.ShoppingListWithItems
import it.matato.dietreminder.data.export.CourseExport
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.export.DietJsonCodec
import it.matato.dietreminder.data.export.FoodItemExport
import it.matato.dietreminder.data.export.MealExport
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import it.matato.dietreminder.data.model.VersionPolicy
import it.matato.dietreminder.data.repository.contracts.ConfigRepository
import it.matato.dietreminder.data.repository.contracts.DietImportItemConfig
import it.matato.dietreminder.data.repository.contracts.DietRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListInitialItem
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository
import it.matato.dietreminder.domain.QuantityAggregator
import it.matato.dietreminder.domain.toIngredientSummaries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDietRepository(
	private val jsonCodec: DietJsonCodec = DietJsonCodec(),
) : DietRepository, ShoppingListRepository, ConfigRepository, VersionPolicyRepository {

	override suspend fun fetchPolicy(): Pair<VersionPolicy, Boolean> = VersionPolicy() to false

	private val _diets = MutableStateFlow<List<Diet>>(emptyList())
	override val all: Flow<List<Diet>> = _diets

	override val active: Flow<Diet?> = _diets.map { list -> list.find { it.isActive } }

	private val _defaultTimes = MutableStateFlow<List<MealDefaultTime>>(emptyList())
	override val defaultTimes: Flow<List<MealDefaultTime>> = _defaultTimes

	private val _config = MutableStateFlow<Map<ConfigKey, String>>(emptyMap())

	private val _meals = MutableStateFlow<Map<Long, List<MealWithDetails>>>(emptyMap())

	private val _shoppingLists = MutableStateFlow<List<ShoppingListWithItems>>(emptyList())
	override val allShoppingLists: Flow<List<ShoppingListWithItems>> = _shoppingLists

	private var currentDietId = 1L
	private var currentMealId = 1L
	private var currentShoppingListId = 1L
	private var currentShoppingItemId = 1L

	override fun observeShoppingList(id: Long): Flow<ShoppingListWithItems?> {
		return _shoppingLists.map { lists -> lists.find { it.list.id == id } }
	}

	override suspend fun createShoppingList(
		name: String,
		dietId: Long?,
		items: List<ShoppingListInitialItem>,
	): Long {
		val listId = currentShoppingListId++
		val newList = ShoppingList(id = listId, name = name, dietId = dietId)
		val createdItems = items.map { initial ->
			ShoppingListItemWithDays(
				item = ShoppingListItem(
					id = currentShoppingItemId++,
					shoppingListId = listId,
					name = initial.name,
					amount = initial.amount,
					unit = initial.unit,
					isBought = false,
					isCustom = true,
				),
				days = emptyList(),
			)
		}
		val withItems = ShoppingListWithItems(list = newList, items = createdItems)
		_shoppingLists.value = listOf(withItems) + _shoppingLists.value
		return listId
	}

	override suspend fun updateShoppingListName(id: Long, name: String) {
		_shoppingLists.value = _shoppingLists.value.map { item ->
			if (item.list.id == id) {
				item.copy(list = item.list.copy(name = name))
			} else {
				item
			}
		}
	}

	override suspend fun deleteShoppingList(id: Long) {
		_shoppingLists.value = _shoppingLists.value.filterNot { it.list.id == id }
	}

	override suspend fun addShoppingListItem(
		listId: Long,
		name: String,
		amount: String,
		unit: QuantityUnit,
		isCustom: Boolean,
	): Long {
		val itemId = currentShoppingItemId++
		val newItem = ShoppingListItemWithDays(
			item = ShoppingListItem(
				id = itemId,
				shoppingListId = listId,
				name = name,
				amount = amount,
				unit = unit,
				isBought = false,
				isCustom = isCustom,
			),
			days = emptyList(),
		)
		_shoppingLists.value = _shoppingLists.value.map { listWithItems ->
			if (listWithItems.list.id == listId) {
				listWithItems.copy(items = listWithItems.items + newItem)
			} else {
				listWithItems
			}
		}
		return itemId
	}

	override suspend fun updateShoppingListItem(item: ShoppingListItem) {
		_shoppingLists.value = _shoppingLists.value.map { listWithItems ->
			val updatedItems = listWithItems.items.map { itemWithDays ->
				if (itemWithDays.item.id == item.id) itemWithDays.copy(item = item) else itemWithDays
			}
			listWithItems.copy(items = updatedItems)
		}
	}

	override suspend fun toggleShoppingListItemBought(itemId: Long, isBought: Boolean) {
		_shoppingLists.value = _shoppingLists.value.map { listWithItems ->
			val updatedItems = listWithItems.items.map { itemWithDays ->
				if (itemWithDays.item.id == itemId) {
					val updatedDays = itemWithDays.days.map { it.copy(isBought = isBought) }
					itemWithDays.copy(
						item = itemWithDays.item.copy(isBought = isBought),
						days = updatedDays,
					)
				} else {
					itemWithDays
				}
			}
			listWithItems.copy(items = updatedItems)
		}
	}

	override suspend fun toggleShoppingListItemDayBought(dayId: Long, isBought: Boolean) {
		_shoppingLists.value = _shoppingLists.value.map { listWithItems ->
			val updatedItems = listWithItems.items.map { itemWithDays ->
				if (itemWithDays.days.any { it.id == dayId }) {
					val updatedDays = itemWithDays.days.map { day ->
						if (day.id == dayId) day.copy(isBought = isBought) else day
					}
					val allBought = updatedDays.isNotEmpty() && updatedDays.all { it.isBought }
					itemWithDays.copy(
						item = itemWithDays.item.copy(isBought = allBought),
						days = updatedDays,
					)
				} else {
					itemWithDays
				}
			}
			listWithItems.copy(items = updatedItems)
		}
	}

	override suspend fun deleteShoppingListItem(itemId: Long) {
		_shoppingLists.value = _shoppingLists.value.map { listWithItems ->
			listWithItems.copy(items = listWithItems.items.filterNot { it.item.id == itemId })
		}
	}

	override suspend fun addDietIngredientsToShoppingList(
		listId: Long,
		dietId: Long,
		configs: List<DietImportItemConfig>,
	) {
		val dietMeals = _meals.value[dietId] ?: emptyList()
		val summaries = dietMeals.toIngredientSummaries()
		val configMap = configs.associateBy { it.ingredientName }

		val itemsToAdd = mutableListOf<ShoppingListItemWithDays>()

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
					val itemId = currentShoppingItemId++
					val newItem = ShoppingListItem(
						id = itemId,
						shoppingListId = listId,
						name = summary.name,
						isBought = false,
						isCustom = false,
						isFresh = true,
					)

					val dayEntities = targetDays.map { (dayOfWeek, occurrences) ->
						val dayAgg = QuantityAggregator.aggregateOccurrences(occurrences)
						ShoppingListItemDay(
							id = currentShoppingItemId++,
							shoppingListItemId = itemId,
							dayOfWeek = dayOfWeek,
							amount = dayAgg.amount,
							unit = dayAgg.unit,
							isBought = false,
						)
					}

					itemsToAdd.add(ShoppingListItemWithDays(item = newItem, days = dayEntities))
				}
			} else {
				val totalAgg = QuantityAggregator.aggregateOccurrences(summary.occurrences)
				val itemId = currentShoppingItemId++
				val newItem = ShoppingListItem(
					id = itemId,
					shoppingListId = listId,
					name = summary.name,
					amount = totalAgg.amount,
					unit = totalAgg.unit,
					isBought = false,
					isCustom = false,
					isFresh = false,
				)
				itemsToAdd.add(ShoppingListItemWithDays(item = newItem, days = emptyList()))
			}
		}

		_shoppingLists.value = _shoppingLists.value.map { listWithItems ->
			if (listWithItems.list.id == listId) {
				listWithItems.copy(items = listWithItems.items + itemsToAdd)
			} else {
				listWithItems
			}
		}
	}

	override suspend fun saveDefaultTime(type: MealType, timeMinutes: Int) {
		val current = _defaultTimes.value.toMutableList()
		current.removeAll { it.type == type }
		current.add(MealDefaultTime(type = type, timeMinutes = timeMinutes))
		_defaultTimes.value = current
	}

	override fun observeConfig(key: ConfigKey): Flow<AppConfig?> {
		return _config.map { map ->
			map[key]?.let { AppConfig(key = key, value = it) }
		}
	}

	override suspend fun saveConfig(key: ConfigKey, value: String) {
		val current = _config.value.toMutableMap()
		current[key] = value
		_config.value = current
	}

	override fun observeMeals(dietId: Long): Flow<List<MealWithDetails>> {
		return _meals.map { it[dietId] ?: emptyList() }
	}

	override suspend fun getMeals(dietId: Long): List<MealWithDetails> {
		return _meals.value[dietId] ?: emptyList()
	}

	override suspend fun create(name: String, window: Int): Long {
		val id = currentDietId++
		val newDiet = Diet(id = id, name = name, nextMealWindowMinutes = window, isActive = _diets.value.isEmpty())
		_diets.value += newDiet
		return id
	}

	override suspend fun updateDiet(diet: Diet) {
		_diets.value = _diets.value.map {
			if (it.id == diet.id) diet else it
		}
	}

	override suspend fun activate(id: Long) {
		_diets.value = _diets.value.map {
			it.copy(isActive = it.id == id)
		}
	}

	override suspend fun delete(id: Long): Int {
		val before = _diets.value.size
		_diets.value = _diets.value.filterNot { it.id == id }
		val deleted = before - _diets.value.size
		if (deleted > 0 && _diets.value.isNotEmpty() && _diets.value.none { it.isActive }) {
			val first = _diets.value.first()
			_diets.value = _diets.value.map {
				if (it.id == first.id) it.copy(isActive = true) else it
			}
		}
		return deleted
	}

	override suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>) {
		val dietId = meal.dietId
		val currentMeals = (_meals.value[dietId] ?: emptyList()).toMutableList()

		val mealToSave = if (meal.id == 0L) {
			meal.copy(id = currentMealId++)
		} else {
			meal
		}

		val details = MealWithDetails(
			meal = mealToSave,
			courses = mealCourses.map { courseWithItems ->
				courseWithItems.copy(
					course = courseWithItems.course.copy(mealId = mealToSave.id)
				)
			}
		)

		currentMeals.removeAll { it.meal.id == mealToSave.id }
		currentMeals.add(details)

		val updatedMap = _meals.value.toMutableMap()
		updatedMap[dietId] = currentMeals
		_meals.value = updatedMap
	}

	override suspend fun deleteMeal(id: Long): Int {
		var totalDeleted = 0
		val updatedMap = _meals.value.toMutableMap()
		updatedMap.forEach { (dietId, list) ->
			val before = list.size
			val filtered = list.filterNot { it.meal.id == id }
			if (filtered.size < before) {
				totalDeleted += (before - filtered.size)
				updatedMap[dietId] = filtered
			}
		}
		_meals.value = updatedMap
		return totalDeleted
	}

	override suspend fun resetDatabase() {
		_diets.value = emptyList()
		_defaultTimes.value = emptyList()
		_config.value = emptyMap()
		_meals.value = emptyMap()
		_shoppingLists.value = emptyList()
	}

	override suspend fun duplicate(id: Long): Long {
		val source = _diets.value.find { it.id == id } ?: throw IllegalArgumentException("Diet not found: $id")
		val newId = currentDietId++
		val duplicated = source.copy(id = newId, name = "${source.name} copia", isActive = false)
		_diets.value += duplicated

		val sourceMeals = _meals.value[id] ?: emptyList()
		val duplicatedMeals = sourceMeals.map { mealDetails ->
			val newMealId = currentMealId++
			MealWithDetails(
				meal = mealDetails.meal.copy(id = newMealId, dietId = newId),
				courses = mealDetails.courses.map { courseWithItems ->
					courseWithItems.copy(
						course = courseWithItems.course.copy(mealId = newMealId)
					)
				}
			)
		}

		val updatedMap = _meals.value.toMutableMap()
		updatedMap[newId] = duplicatedMeals
		_meals.value = updatedMap

		return newId
	}

	override suspend fun exportJson(id: Long): String {
		val diet = _diets.value.find { it.id == id } ?: throw IllegalArgumentException("Diet not found: $id")
		val dietMeals = _meals.value[id] ?: emptyList()

		val export = DietExport(
			uuid = diet.uuid,
			name = diet.name,
			nextMealWindowMinutes = diet.nextMealWindowMinutes,
			disabledNotificationDays = diet.disabledNotificationDays,
			meals = dietMeals.map { mealWithDetails ->
				MealExport(
					day = mealWithDetails.meal.dayOfWeek,
					type = mealWithDetails.meal.type,
					time = "%02d:%02d".format(
						mealWithDetails.meal.timeMinutes / 60,
						mealWithDetails.meal.timeMinutes % 60,
					),
					description = mealWithDetails.meal.description,
					customTypeLabel = mealWithDetails.meal.customTypeLabel,
					isNotificationEnabled = mealWithDetails.meal.isNotificationEnabled,
					courses = mealWithDetails.courses.map { courseWithItems ->
						CourseExport(
							name = courseWithItems.course.name,
							order = courseWithItems.course.order,
							items = courseWithItems.items.map { item ->
								FoodItemExport(
									name = item.name,
									amount = item.amount,
									unit = item.unit,
									order = item.order,
								)
							}
						)
					}
				)
			}
		)
		return jsonCodec.encode(export)
	}

	override fun parseDietJson(json: String): DietExport = jsonCodec.decode(json)

	override suspend fun dietExists(uuid: String): Boolean {
		return _diets.value.any { it.uuid == uuid }
	}

	private fun parseTime(time: String): Int {
		val parts = time.split(":")
		return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
	}

	override suspend fun importJson(json: String, overwrite: Boolean) {
		val export = parseDietJson(json)
		val existing = export.uuid?.let { uuid -> _diets.value.find { it.uuid == uuid } }

		if (existing != null && overwrite) {
			delete(existing.id)
		}

		val newDietId = create(export.name, export.nextMealWindowMinutes)
		_diets.value = _diets.value.map {
			if (it.id == newDietId) it.copy(
				uuid = export.uuid ?: it.uuid,
				disabledNotificationDays = export.disabledNotificationDays) else it
		}

		export.meals.forEach { mealExport ->
			val meal = Meal(
				dietId = newDietId,
				type = mealExport.type,
				timeMinutes = parseTime(mealExport.time),
				dayOfWeek = mealExport.day,
				description = mealExport.description,
				customTypeLabel = mealExport.customTypeLabel,
				isNotificationEnabled = mealExport.isNotificationEnabled
			)
			val courses = mealExport.courses.map { courseExport ->
				CourseWithItems(
					course = Course(mealId = 0L, name = courseExport.name, order = courseExport.order),
					items = courseExport.items.map { itemExport ->
						FoodItem(
							courseId = 0L,
							name = itemExport.name,
							amount = itemExport.amount,
							unit = itemExport.unit,
							order = itemExport.order,
						)
					}
				)
			}
			saveMeal(meal, courses)
		}
	}
}
