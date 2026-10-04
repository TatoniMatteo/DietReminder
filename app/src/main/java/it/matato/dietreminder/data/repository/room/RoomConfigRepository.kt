package it.matato.dietreminder.data.repository.room

import androidx.room.withTransaction
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.dao.ConfigDao
import it.matato.dietreminder.data.database.dao.DietDao
import it.matato.dietreminder.data.database.dao.ShoppingListDao
import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.repository.contracts.ConfigRepository
import kotlinx.coroutines.flow.Flow

class RoomConfigRepository(
	private val database: AppDatabase,
	private val config: ConfigDao,
	private val diets: DietDao,
	private val shoppingLists: ShoppingListDao,
) : ConfigRepository {

	override val defaultTimes: Flow<List<MealDefaultTime>> = config.observeDefaultTimes()

	override suspend fun saveDefaultTime(type: MealType, timeMinutes: Int) {
		config.insertDefaultTime(MealDefaultTime(type = type, timeMinutes = timeMinutes))
	}

	override fun observeConfig(key: ConfigKey): Flow<AppConfig?> = config.observeConfig(key)

	override suspend fun saveConfig(key: ConfigKey, value: String) {
		config.insertConfig(AppConfig(key = key, value = value))
	}

	override suspend fun resetDatabase() = database.withTransaction {
		diets.deleteAll()
		config.deleteAllTimes()
		config.deleteAll()
		shoppingLists.deleteAllLists()
	}
}
