package it.matato.dietreminder.data.repository

import androidx.room.withTransaction
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.dao.ConfigDao
import it.matato.dietreminder.data.database.dao.CourseDao
import it.matato.dietreminder.data.database.dao.DietDao
import it.matato.dietreminder.data.database.dao.FoodItemDao
import it.matato.dietreminder.data.database.dao.MealDao
import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.export.CourseExport
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.export.DietJsonCodec
import it.matato.dietreminder.data.export.FoodItemExport
import it.matato.dietreminder.data.export.MealExport
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.util.AppLog
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class DietRepository(
    private val database: AppDatabase,
    private val diets: DietDao,
    private val meals: MealDao,
    private val courses: CourseDao,
    private val foodItems: FoodItemDao,
    private val config: ConfigDao,
    private val jsonCodec: DietJsonCodec = DietJsonCodec()
) {

    val all: Flow<List<Diet>> = diets.observeAll()

    val active: Flow<Diet?> = diets.observeActive()

    val defaultTimes: Flow<List<MealDefaultTime>> =
        config.observeDefaultTimes()

    suspend fun saveDefaultTime(type: MealType, timeMinutes: Int) {
        config.insertDefaultTime(
            MealDefaultTime(
                type = type,
                timeMinutes = timeMinutes
            )
        )
    }

    fun observeConfig(key: String): Flow<AppConfig?> =
        config.observeConfig(key)

    suspend fun saveConfig(key: String, value: String) {
        config.insertConfig(
            AppConfig(
                key = key,
                value = value
            )
        )
    }

    fun observeMeals(dietId: Long) =
        meals.observeWithDetails(dietId)

    suspend fun getMeals(dietId: Long) =
        meals.getAllWithDetails(dietId)

    suspend fun create(name: String, window: Int): Long =
        diets.insert(
            Diet(
                name = name,
                nextMealWindowMinutes = window
            )
        )

    suspend fun activate(id: Long) {
        database.withTransaction {
            diets.deactivateAll()
            diets.activate(id)
        }
    }

    suspend fun delete(id: Long): Int =
        diets.delete(id)

    suspend fun saveMeal(
        meal: Meal,
        mealCourses: List<CourseWithItems>
    ) {
        database.withTransaction {
            AppLog.i(
                "Saving meal: Type=${meal.type}, Time=${meal.timeMinutes}min"
            )

            val mealId = if (meal.id == 0L) {
                val id = meals.insert(meal)
                AppLog.d("Inserted new meal with ID: $id")
                id
            } else {
                meals.update(meal)

                val existingCourses = courses.getForMeal(meal.id)

                AppLog.t(
                    "Clearing ${existingCourses.size} courses " +
                            "for meal ID: ${meal.id}"
                )

                existingCourses.forEach { course ->
                    courses.delete(course.id)
                }

                meal.id
            }

            mealCourses.forEachIndexed { courseIndex, courseWithItems ->
                val courseId = courses.insert(
                    courseWithItems.course.copy(
                        id = 0,
                        mealId = mealId,
                        order = courseIndex
                    )
                )

                AppLog.t(
                    "Saved course: ${courseWithItems.course.name} " +
                            "(ID: $courseId)"
                )

                courseWithItems.items.forEachIndexed { itemIndex, item ->
                    foodItems.insert(
                        item.copy(
                            id = 0,
                            courseId = courseId,
                            order = itemIndex
                        )
                    )
                }
            }

            AppLog.i("Meal save transaction finished")
        }
    }

    suspend fun deleteMeal(id: Long): Int =
        meals.delete(id)

    suspend fun resetDatabase() {
        database.withTransaction {
            diets.deleteAll()
            config.deleteAll()
            config.deleteAllTimes()
        }
    }

    suspend fun duplicate(id: Long): Long =
        database.withTransaction {
            val source = diets.get(id)
                ?: error("Diet not found")

            val newDietId = diets.insert(
                source.copy(
                    id = 0,
                    name = "${source.name} copia",
                    isActive = false
                )
            )

            val mealDetails = meals.getAllWithDetails(id)

            mealDetails.forEach { mealDetails ->
                val newMealId = meals.insert(
                    mealDetails.meal.copy(
                        id = 0,
                        dietId = newDietId
                    )
                )

                mealDetails.courses.forEach { courseDetails ->
                    val newCourseId = courses.insert(
                        courseDetails.course.copy(
                            id = 0,
                            mealId = newMealId
                        )
                    )

                    courseDetails.items.forEach { item ->
                        foodItems.insert(
                            item.copy(
                                id = 0,
                                courseId = newCourseId
                            )
                        )
                    }
                }
            }

            newDietId
        }

    suspend fun exportJson(id: Long): String {
        val diet = diets.get(id)
            ?: return ""

        val mealDetails = meals.getAllWithDetails(id)

        val export = DietExport(
            uuid = diet.uuid,
            name = diet.name,
            nextMealWindowMinutes = diet.nextMealWindowMinutes,
            meals = mealDetails.map { mealDetails ->
                MealExport(
                    day = mealDetails.meal.dayOfWeek,
                    type = mealDetails.meal.type,
                    time = "%02d:%02d".format(
                        mealDetails.meal.timeMinutes / 60,
                        mealDetails.meal.timeMinutes % 60
                    ),
                    description = mealDetails.meal.description,
                    customTypeLabel = mealDetails.meal.customTypeLabel,
                    courses = mealDetails.courses.map { courseDetails ->
                        CourseExport(
                            name = courseDetails.course.name,
                            order = courseDetails.course.order,
                            items = courseDetails.items.map { item ->
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

    fun parseDietJson(json: String): DietExport =
        jsonCodec.decode(json)

    suspend fun dietExists(uuid: String): Boolean =
        diets.getByUuid(uuid) != null

    suspend fun importJson(
        json: String,
        overwrite: Boolean = false
    ) {
        database.withTransaction {
            try {
                AppLog.d("Starting import process...")

                val data = parseDietJson(json)
                val existing = data.uuid?.let { diets.getByUuid(it) }

                if (existing != null) {
                    if (overwrite) {
                        AppLog.d(
                            "Overwriting existing diet: ${existing.name}"
                        )
                        diets.delete(existing.id)
                    } else {
                        AppLog.d(
                            "Diet with UUID ${data.uuid} already exists, " +
                                    "skipping."
                        )
                        return@withTransaction
                    }
                }

                val dietId = diets.insert(
                    Diet(
                        uuid = data.uuid ?: UUID.randomUUID().toString(),
                        name = data.name,
                        nextMealWindowMinutes = data.nextMealWindowMinutes
                    )
                )

                AppLog.d("Diet inserted with ID: $dietId")

                data.meals.forEach { meal ->
                    val mealId = meals.insert(
                        Meal(
                            dietId = dietId,
                            dayOfWeek = meal.day,
                            type = meal.type,
                            timeMinutes = parseTime(meal.time),
                            description = meal.description,
                            customTypeLabel = meal.customTypeLabel
                        )
                    )

                    meal.courses.forEach { course ->
                        val courseId = courses.insert(
                            Course(
                                mealId = mealId,
                                name = course.name,
                                order = course.order
                            )
                        )

                        course.items.forEach { item ->
                            foodItems.insert(
                                FoodItem(
                                    courseId = courseId,
                                    name = item.name,
                                    quantities = item.quantities,
                                    order = item.order
                                )
                            )
                        }
                    }
                }

                AppLog.d("Import completed successfully")
            } catch (e: Exception) {
                AppLog.e("Error during importJson", e)
                throw e
            }
        }
    }

    private fun parseTime(value: String): Int {
        val parts = value.split(":")

        if (parts.size != 2) {
            return 0
        }

        val hours = parts[0].trim().toIntOrNull() ?: 0
        val minutes = parts[1].trim().toIntOrNull() ?: 0

        return hours * 60 + minutes
    }
}