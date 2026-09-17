package it.matato.dietreminder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import it.matato.dietreminder.data.database.entity.Diet
import kotlinx.coroutines.flow.Flow

@Dao
interface DietDao {

    @Query("SELECT * FROM diets ORDER BY isActive DESC, name")
    fun observeAll(): Flow<List<Diet>>

    @Query("SELECT * FROM diets WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<Diet?>

    @Query("SELECT * FROM diets WHERE id = :id")
    suspend fun get(id: Long): Diet?

    @Query("SELECT * FROM diets WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): Diet?

    @Insert
    suspend fun insert(diet: Diet): Long

    @Update
    suspend fun update(diet: Diet): Int

    @Query("DELETE FROM diets WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("UPDATE diets SET isActive = 0")
    suspend fun deactivateAll(): Int

    @Query("UPDATE diets SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long): Int

    @Query("DELETE FROM diets")
    suspend fun deleteAll()
}