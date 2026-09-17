package it.matato.dietreminder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.MealWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Transaction
    @Query(
        """
        SELECT * FROM meals
        WHERE dietId = :dietId
        ORDER BY dayOfWeek, timeMinutes
    """
    )
    fun observeWithDetails(dietId: Long): Flow<List<MealWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM meals
        WHERE dietId = :dietId
        ORDER BY dayOfWeek, timeMinutes
    """
    )
    suspend fun getAllWithDetails(dietId: Long): List<MealWithDetails>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun get(id: Long): Meal?

    @Insert
    suspend fun insert(meal: Meal): Long

    @Update
    suspend fun update(meal: Meal): Int

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: Long): Int
}