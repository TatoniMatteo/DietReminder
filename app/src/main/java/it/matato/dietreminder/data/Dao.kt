package it.matato.dietreminder.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DietDao {
    @Query("SELECT * FROM diets ORDER BY isActive DESC, name")
    fun observeAll(): Flow<List<DietEntity>>
    @Query("SELECT * FROM diets WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<DietEntity?>
    @Query("SELECT * FROM diets WHERE id = :id")
    suspend fun get(id: Long): DietEntity?
    @Query("SELECT * FROM diets WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): DietEntity?
    @Insert
    suspend fun insert(diet: DietEntity): Long
    @Update
    suspend fun update(diet: DietEntity): Int
    @Query("DELETE FROM diets WHERE id = :id")
    suspend fun delete(id: Long): Int
    @Query("UPDATE diets SET isActive = 0")
    suspend fun deactivateAll(): Int
    @Query("UPDATE diets SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long): Int
    @Query("DELETE FROM diets")
    suspend fun deleteAll()
}

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals WHERE dietId = :dietId ORDER BY dayOfWeek, timeMinutes")
    fun observeWithDetails(dietId: Long): Flow<List<MealWithDetails>>

    @Transaction
    @Query("SELECT * FROM meals WHERE dietId = :dietId")
    suspend fun getAllWithDetails(dietId: Long): List<MealWithDetails>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun get(id: Long): MealEntity?

    @Insert
    suspend fun insert(meal: MealEntity): Long
    @Update
    suspend fun update(meal: MealEntity): Int
    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: Long): Int
}

@Dao
interface CourseDao {
    @Insert
    suspend fun insert(course: CourseEntity): Long
    @Update
    suspend fun update(course: CourseEntity): Int
    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT * FROM courses WHERE mealId = :mealId ORDER BY `order` ASC")
    suspend fun getForMeal(mealId: Long): List<CourseEntity>
}

@Dao
interface FoodItemDao {
    @Insert
    suspend fun insert(item: FoodItemEntity): Long
    @Update
    suspend fun update(item: FoodItemEntity): Int
    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT * FROM food_items WHERE courseId = :courseId ORDER BY `order` ASC")
    suspend fun getForCourse(courseId: Long): List<FoodItemEntity>
}

@Dao
interface ConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultTime(time: MealDefaultTimeEntity)

    @Query("SELECT * FROM meal_default_times")
    fun observeDefaultTimes(): Flow<List<MealDefaultTimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfigEntity)

    @Query("SELECT * FROM app_config WHERE `key` = :key")
    fun observeConfig(key: String): Flow<AppConfigEntity?>

    @Query("DELETE FROM app_config")
    suspend fun deleteAll()

    @Query("DELETE FROM meal_default_times")
    suspend fun deleteAllTimes()
}

data class MealWithDetails(
    @Embedded val meal: MealEntity,
    @Relation(
        entity = CourseEntity::class,
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val courses: List<CourseWithItems>
)

data class CourseWithItems(
    @Embedded val course: CourseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val items: List<FoodItemEntity>
)
