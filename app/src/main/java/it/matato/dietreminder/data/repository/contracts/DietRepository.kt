package it.matato.dietreminder.data.repository.contracts

import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.export.DietExport
import kotlinx.coroutines.flow.Flow

interface DietReadRepository {
	val all: Flow<List<Diet>>
	val active: Flow<Diet?>

	fun observeMeals(dietId: Long): Flow<List<MealWithDetails>>
	suspend fun getMeals(dietId: Long): List<MealWithDetails>
	suspend fun exportJson(id: Long): String
	fun parseDietJson(json: String): DietExport
	suspend fun dietExists(uuid: String): Boolean
}

interface DietWriteRepository {
	suspend fun create(name: String, window: Int): Long
	suspend fun updateDiet(diet: Diet)
	suspend fun activate(id: Long)
	suspend fun delete(id: Long): Int
	suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>)
	suspend fun deleteMeal(id: Long): Int
	suspend fun duplicate(id: Long): Long
	suspend fun importJson(json: String, overwrite: Boolean = false)
}

interface DietRepository : DietReadRepository, DietWriteRepository
