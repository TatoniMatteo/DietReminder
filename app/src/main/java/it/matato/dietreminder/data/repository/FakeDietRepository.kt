package it.matato.dietreminder.data.repository

import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.export.CourseExport
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.export.DietJsonCodec
import it.matato.dietreminder.data.export.FoodItemExport
import it.matato.dietreminder.data.export.MealExport
import it.matato.dietreminder.data.model.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDietRepository : DietRepository {

    private val _diets = MutableStateFlow<List<Diet>>(emptyList())
    private val _config = MutableStateFlow<Map<ConfigKey, String>>(emptyMap())
    private val _defaultTimes = MutableStateFlow<List<MealDefaultTime>>(emptyList())
    private val _meals = MutableStateFlow<Map<Long, List<MealWithDetails>>>(emptyMap())
    private val jsonCodec = DietJsonCodec()

    override val all: Flow<List<Diet>> = _diets
    override val active: Flow<Diet?> = _diets.map { list -> list.find { it.isActive } }
    override val defaultTimes: Flow<List<MealDefaultTime>> = _defaultTimes

    private var currentDietId = 1L
    private var currentMealId = 1L

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
        _diets.value = _diets.value + newDiet
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
        val currentMeals = _meals.value.toMutableMap()
        currentMeals.remove(id)
        _meals.value = currentMeals
        return before - _diets.value.size
    }

    override suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>) {
        val currentMap = _meals.value.toMutableMap()
        val dietMeals = currentMap[meal.dietId]?.toMutableList() ?: mutableListOf()

        val mealToSave = if (meal.id == 0L) {
            meal.copy(id = currentMealId++)
        } else {
            meal
        }

        val updatedCourses = mealCourses.map { courseWithItems ->
            courseWithItems.copy(
                course = courseWithItems.course.copy(mealId = mealToSave.id)
            )
        }

        val mealWithDetails = MealWithDetails(meal = mealToSave, courses = updatedCourses)

        val index = dietMeals.indexOfFirst { it.meal.id == mealToSave.id }
        if (index >= 0) {
            dietMeals[index] = mealWithDetails
        } else {
            dietMeals.add(mealWithDetails)
        }

        currentMap[meal.dietId] = dietMeals
        _meals.value = currentMap
    }

    override suspend fun deleteMeal(id: Long): Int {
        val currentMap = _meals.value.toMutableMap()
        var deletedCount = 0

        for ((dietId, list) in currentMap) {
            val filtered = list.filterNot { it.meal.id == id }
            if (filtered.size != list.size) {
                deletedCount += (list.size - filtered.size)
                currentMap[dietId] = filtered
            }
        }

        _meals.value = currentMap
        return deletedCount
    }

    override suspend fun resetDatabase() {
        _diets.value = emptyList()
        _config.value = emptyMap()
        _defaultTimes.value = emptyList()
        _meals.value = emptyMap()
    }

    override suspend fun duplicate(id: Long): Long {
        val source = _diets.value.find { it.id == id } ?: return 0L
        val newId = currentDietId++
        val duplicated = source.copy(id = newId, name = "${source.name} copia", isActive = false)
        _diets.value = _diets.value + duplicated

        val sourceMeals = _meals.value[id] ?: emptyList()
        if (sourceMeals.isNotEmpty()) {
            val duplicatedMeals = sourceMeals.map { sourceMealWithDetails ->
                val newMealId = currentMealId++
                val duplicatedMeal = sourceMealWithDetails.meal.copy(id = newMealId, dietId = newId)
                val duplicatedCourses = sourceMealWithDetails.courses.map { courseWithItems ->
                    courseWithItems.copy(
                        course = courseWithItems.course.copy(mealId = newMealId)
                    )
                }
                MealWithDetails(meal = duplicatedMeal, courses = duplicatedCourses)
            }
            val currentMap = _meals.value.toMutableMap()
            currentMap[newId] = duplicatedMeals
            _meals.value = currentMap
        }

        return newId
    }

    override suspend fun exportJson(id: Long): String {
        val diet = _diets.value.find { it.id == id } ?: return ""
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
                                    quantities = item.quantities,
                                    order = item.order
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
            if (it.id == newDietId) it.copy(uuid = export.uuid ?: it.uuid, disabledNotificationDays = export.disabledNotificationDays) else it
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
                            quantities = itemExport.quantities,
                            order = itemExport.order
                        )
                    }
                )
            }
            saveMeal(meal, courses)
        }
    }
}
