package it.matato.dietreminder.data.repository.contracts

import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.model.MealType
import kotlinx.coroutines.flow.Flow

interface ConfigReadRepository {
	val defaultTimes: Flow<List<MealDefaultTime>>

	fun observeConfig(key: ConfigKey): Flow<AppConfig?>
}

interface ConfigWriteRepository {
	suspend fun saveDefaultTime(type: MealType, timeMinutes: Int)
	suspend fun saveConfig(key: ConfigKey, value: String)
	suspend fun resetDatabase()
}

interface ConfigRepository : ConfigReadRepository, ConfigWriteRepository
