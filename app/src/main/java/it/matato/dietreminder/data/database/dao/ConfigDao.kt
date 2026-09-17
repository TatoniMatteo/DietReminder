package it.matato.dietreminder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultTime(time: MealDefaultTime)

    @Query("SELECT * FROM meal_default_times")
    fun observeDefaultTimes(): Flow<List<MealDefaultTime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfig)

    @Query("SELECT * FROM app_config WHERE `key` = :key")
    fun observeConfig(key: String): Flow<AppConfig?>

    @Query("DELETE FROM app_config")
    suspend fun deleteAll()

    @Query("DELETE FROM meal_default_times")
    suspend fun deleteAllTimes()
}