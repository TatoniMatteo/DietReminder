package it.matato.dietreminder.data.repository.delegating

import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.repository.contracts.DietReadRepository
import it.matato.dietreminder.data.repository.contracts.DietRepository
import kotlinx.coroutines.flow.Flow

class DelegatingDietRepository(
	private val onlineRepository: DietRepository,
	private val localCacheRepository: DietReadRepository,
	private val isOfflineProvider: () -> Boolean,
) : DietRepository {

	private val currentRepo: DietReadRepository
		get() = if (isOfflineProvider()) localCacheRepository else onlineRepository

	override val all: Flow<List<Diet>> get() = currentRepo.all
	override val active: Flow<Diet?> get() = currentRepo.active

	override fun observeMeals(dietId: Long): Flow<List<MealWithDetails>> = currentRepo.observeMeals(dietId)

	override suspend fun getMeals(dietId: Long): List<MealWithDetails> = currentRepo.getMeals(dietId)

	override suspend fun create(name: String, window: Int): Long {
		requireOnlineWrites(isOfflineProvider)
		return onlineRepository.create(name, window)
	}

	override suspend fun updateDiet(diet: Diet) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.updateDiet(diet)
	}

	override suspend fun activate(id: Long) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.activate(id)
	}

	override suspend fun delete(id: Long): Int {
		requireOnlineWrites(isOfflineProvider)
		return onlineRepository.delete(id)
	}

	override suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.saveMeal(meal, mealCourses)
	}

	override suspend fun deleteMeal(id: Long): Int {
		requireOnlineWrites(isOfflineProvider)
		return onlineRepository.deleteMeal(id)
	}

	override suspend fun duplicate(id: Long): Long {
		requireOnlineWrites(isOfflineProvider)
		return onlineRepository.duplicate(id)
	}

	override suspend fun exportJson(id: Long): String = currentRepo.exportJson(id)

	override fun parseDietJson(json: String): DietExport = currentRepo.parseDietJson(json)

	override suspend fun dietExists(uuid: String): Boolean = currentRepo.dietExists(uuid)

	override suspend fun importJson(json: String, overwrite: Boolean) {
		requireOnlineWrites(isOfflineProvider)
		onlineRepository.importJson(json, overwrite)
	}
}
