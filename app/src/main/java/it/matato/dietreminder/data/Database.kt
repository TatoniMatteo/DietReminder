package it.matato.dietreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import it.matato.dietreminder.util.AppLog
import java.util.UUID
import kotlinx.serialization.json.Json

@Database(
    entities = [
        DietEntity::class,
        MealEntity::class,
        CourseEntity::class,
        FoodItemEntity::class,
        MealDefaultTimeEntity::class,
        AppConfigEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dietDao(): DietDao
    abstract fun mealDao(): MealDao
    abstract fun courseDao(): CourseDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun configDao(): ConfigDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "diet.db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }
}

class DietRepository(
    private val diets: DietDao,
    private val meals: MealDao,
    private val courses: CourseDao,
    private val foodItems: FoodItemDao,
    private val config: ConfigDao
) {
    val all = diets.observeAll()
    val active = diets.observeActive()
    val defaultTimes = config.observeDefaultTimes()

    suspend fun saveDefaultTime(type: MealType, timeMinutes: Int) =
        config.insertDefaultTime(MealDefaultTimeEntity(type, timeMinutes))

    fun observeConfig(key: String) = config.observeConfig(key)
    suspend fun saveConfig(key: String, value: String) = config.insertConfig(AppConfigEntity(key, value))

    fun observeMeals(id: Long) = meals.observeWithDetails(id)
    suspend fun getMeals(id: Long) = meals.getAllWithDetails(id)

    suspend fun create(name: String, window: Int) =
        diets.insert(DietEntity(name = name, nextMealWindowMinutes = window))

    suspend fun activate(id: Long) {
        diets.deactivateAll()
        diets.activate(id)
    }

    suspend fun delete(id: Long) = diets.delete(id)

    suspend fun saveMeal(meal: MealEntity, mealCourses: List<CourseWithItems>) {
        AppLog.i("Saving meal: Type=${meal.type}, Time=${meal.timeMinutes}min")
        val mealId = if (meal.id == 0L) {
            val id = meals.insert(meal)
            AppLog.d("Inserted new meal with ID: $id")
            id
        } else {
            meals.update(meal)
            val existingCourses = courses.getForMeal(meal.id)
            AppLog.t("Clearing ${existingCourses.size} courses for meal ID: ${meal.id}")
            existingCourses.forEach { c ->
                val items = foodItems.getForCourse(c.id)
                items.forEach { foodItems.delete(it.id) }
                courses.delete(c.id)
            }
            meal.id
        }

        mealCourses.forEachIndexed { cIndex, c ->
            val cId = courses.insert(c.course.copy(id = 0, mealId = mealId, order = cIndex))
            AppLog.t("Saved course: ${c.course.name} (ID: $cId)")
            c.items.forEachIndexed { iIndex, item ->
                foodItems.insert(item.copy(id = 0, courseId = cId, order = iIndex))
            }
        }
        AppLog.i("Meal save transaction finished")
    }

    suspend fun deleteMeal(id: Long) = meals.delete(id)

    suspend fun resetDatabase() {
        diets.deleteAll()
        config.deleteAll()
        config.deleteAllTimes()
    }

    suspend fun duplicate(id: Long): Long {
        val source = diets.get(id) ?: error("Diet not found")
        val newDietId = diets.insert(source.copy(id = 0, name = "${source.name} copia", isActive = false))

        val mealDetails = meals.getAllWithDetails(id)
        mealDetails.forEach { mDetail ->
            val newMealId = meals.insert(mDetail.meal.copy(id = 0, dietId = newDietId))
            mDetail.courses.forEach { cDetail ->
                val newCourseId = courses.insert(cDetail.course.copy(id = 0, mealId = newMealId))
                cDetail.items.forEach { item ->
                    foodItems.insert(item.copy(id = 0, courseId = newCourseId))
                }
            }
        }
        return newDietId
    }

    suspend fun exportJson(id: Long): String {
        val diet = diets.get(id) ?: return ""
        val mealDetails = meals.getAllWithDetails(id)
        val export = DietExport(
            uuid = diet.uuid,
            name = diet.name,
            nextMealWindowMinutes = diet.nextMealWindowMinutes,
            meals = mealDetails.map { m ->
                MealExport(
                    day = m.meal.dayOfWeek,
                    type = m.meal.type,
                    time = "%02d:%02d".format(m.meal.timeMinutes / 60, m.meal.timeMinutes % 60),
                    description = m.meal.description,
                    customTypeLabel = m.meal.customTypeLabel,
                    courses = m.courses.map { c ->
                        CourseExport(
                            name = c.course.name,
                            order = c.course.order,
                            items = c.items.map { i ->
                                FoodItemExport(
                                    name = i.name,
                                    quantities = i.quantities,
                                    order = i.order
                                )
                            }
                        )
                    }
                )
            }
        )
        return Json.encodeToString(export)
    }

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun parseDietJson(json: String): DietExport {
        return jsonConfig.decodeFromString(json)
    }

    suspend fun dietExists(uuid: String): Boolean = diets.getByUuid(uuid) != null

    suspend fun importJson(json: String, overwrite: Boolean = false) {
        try {
            AppLog.d("Starting import process...")
            val data = parseDietJson(json)
            val existing = data.uuid?.let { diets.getByUuid(it) }
            
            if (existing != null) {
                if (overwrite) {
                    AppLog.d("Overwriting existing diet: ${existing.name}")
                    delete(existing.id)
                } else {
                    AppLog.d("Diet with UUID ${data.uuid} already exists, skipping.")
                    return
                }
            }

            val dietId = diets.insert(
                DietEntity(
                    uuid = data.uuid ?: UUID.randomUUID().toString(),
                    name = data.name,
                    nextMealWindowMinutes = data.nextMealWindowMinutes
                )
            )
            AppLog.d("Diet inserted with ID: $dietId")
            
            data.meals.forEach { m ->
                val timeParts = m.time.split(":")
                val minutes = if (timeParts.size == 2) {
                    (timeParts[0].trim().toIntOrNull() ?: 0) * 60 + (timeParts[1].trim().toIntOrNull() ?: 0)
                } else 0

                val mealId = meals.insert(
                    MealEntity(
                        dietId = dietId,
                        dayOfWeek = m.day,
                        type = m.type,
                        timeMinutes = minutes,
                        description = m.description,
                        customTypeLabel = m.customTypeLabel
                    )
                )

                m.courses.forEach { c ->
                    val cId = courses.insert(CourseEntity(mealId = mealId, name = c.name, order = c.order))
                    c.items.forEach { i ->
                        foodItems.insert(FoodItemEntity(courseId = cId, name = i.name, quantities = i.quantities, order = i.order))
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
