package it.matato.dietreminder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import it.matato.dietreminder.data.database.entity.FoodItem

@Dao
interface FoodItemDao {

    @Insert
    suspend fun insert(item: FoodItem): Long

    @Update
    suspend fun update(item: FoodItem): Int

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("""
        SELECT * FROM food_items
        WHERE courseId = :courseId
        ORDER BY `order` ASC
    """)
    suspend fun getForCourse(courseId: Long): List<FoodItem>
}