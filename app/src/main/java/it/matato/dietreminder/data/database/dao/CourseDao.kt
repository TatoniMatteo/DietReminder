package it.matato.dietreminder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import it.matato.dietreminder.data.database.entity.Course

@Dao
interface CourseDao {

    @Insert
    suspend fun insert(course: Course): Long

    @Update
    suspend fun update(course: Course): Int

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query(
        """
        SELECT * FROM courses
        WHERE mealId = :mealId
        ORDER BY `order` ASC
    """
    )
    suspend fun getForMeal(mealId: Long): List<Course>
}