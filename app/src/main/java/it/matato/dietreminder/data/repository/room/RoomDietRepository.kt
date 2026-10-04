package it.matato.dietreminder.data.repository.room

import androidx.room.withTransaction
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.dao.CourseDao
import it.matato.dietreminder.data.database.dao.DietDao
import it.matato.dietreminder.data.database.dao.FoodItemDao
import it.matato.dietreminder.data.database.dao.MealDao
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.export.CourseExport
import it.matato.dietreminder.data.export.DietExport
import it.matato.dietreminder.data.export.DietJsonCodec
import it.matato.dietreminder.data.export.FoodItemExport
import it.matato.dietreminder.data.export.MealExport
import it.matato.dietreminder.data.repository.contracts.DietRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoomDietRepository(
	private val database: AppDatabase,
	private val diets: DietDao,
	private val meals: MealDao,
	private val courses: CourseDao,
	private val foodItems: FoodItemDao,
	private val jsonCodec: DietJsonCodec = DietJsonCodec(),
) : DietRepository {

	override val all: Flow<List<Diet>> = diets.observeAll()

	override val active: Flow<Diet?> = diets.observeActive()

	override fun observeMeals(dietId: Long): Flow<List<MealWithDetails>> =
		meals.observeWithDetails(dietId)

	override suspend fun getMeals(dietId: Long): List<MealWithDetails> =
		meals.getAllWithDetails(dietId)

	override suspend fun create(name: String, window: Int): Long {
		val count = diets.observeAll().first().size
		val diet = Diet(
			name = name,
			nextMealWindowMinutes = window,
			isActive = count == 0,
		)
		return diets.insert(diet)
	}

	override suspend fun updateDiet(diet: Diet) {
		diets.update(diet)
	}

	override suspend fun activate(id: Long) {
		database.withTransaction {
			diets.deactivateAll()
			diets.activate(id)
		}
	}

	override suspend fun delete(id: Long): Int = database.withTransaction {
		val deletedCount = diets.delete(id)
		if (deletedCount > 0) {
			val remaining = diets.observeAll().first()
			if (remaining.isNotEmpty() && remaining.none { it.isActive }) {
				diets.activate(remaining.first().id)
			}
		}
		deletedCount
	}

	override suspend fun saveMeal(meal: Meal, mealCourses: List<CourseWithItems>) =
		database.withTransaction {
			val mealId = if (meal.id == 0L) {
				meals.insert(meal)
			} else {
				meals.update(meal)
				val existingCourses = courses.getForMeal(meal.id)
				existingCourses.forEach { courses.delete(it.id) }
				meal.id
			}

			mealCourses.forEachIndexed { courseIndex, courseWithItems ->
				val courseId = courses.insert(
					Course(
						mealId = mealId,
						name = courseWithItems.course.name,
						order = courseWithIndex(courseIndex, courseWithItems.course.order),
					),
				)

				courseWithItems.items.forEachIndexed { itemIndex, item ->
					foodItems.insert(
						FoodItem(
							courseId = courseId,
							name = item.name,
							amount = item.amount,
							unit = item.unit,
							order = itemIndex,
						),
					)
				}
			}
		}

	private fun courseWithIndex(index: Int, order: Int): Int = if (order != 0) order else index

	override suspend fun deleteMeal(id: Long): Int = meals.delete(id)

	override suspend fun duplicate(id: Long): Long = database.withTransaction {
		val source = diets.get(id) ?: throw IllegalArgumentException("Diet not found: $id")
		val sourceMeals = meals.getAllWithDetails(id)

		val newDietId = diets.insert(
			Diet(
				name = "${source.name} copia",
				nextMealWindowMinutes = source.nextMealWindowMinutes,
				disabledNotificationDays = source.disabledNotificationDays,
				isActive = false,
				uuid = UUID.randomUUID().toString(),
			),
		)

		sourceMeals.forEach { mealDetails ->
			val newMealId = meals.insert(
				Meal(
					dietId = newDietId,
					type = mealDetails.meal.type,
					timeMinutes = mealDetails.meal.timeMinutes,
					dayOfWeek = mealDetails.meal.dayOfWeek,
					description = mealDetails.meal.description,
					customTypeLabel = mealDetails.meal.customTypeLabel,
					isNotificationEnabled = mealDetails.meal.isNotificationEnabled,
				),
			)

			mealDetails.courses.forEach { courseDetails ->
				val newCourseId = courses.insert(
					Course(
						mealId = newMealId,
						name = courseDetails.course.name,
						order = courseDetails.course.order,
					),
				)

				courseDetails.items.forEach { item ->
					foodItems.insert(
						FoodItem(
							courseId = newCourseId,
							name = item.name,
							amount = item.amount,
							unit = item.unit,
							order = item.order,
						),
					)
				}
			}
		}

		newDietId
	}

	override suspend fun exportJson(id: Long): String {
		val diet = diets.get(id) ?: throw IllegalArgumentException("Diet not found: $id")
		val dietMeals = meals.getAllWithDetails(id)

		val export = DietExport(
			uuid = diet.uuid,
			name = diet.name,
			nextMealWindowMinutes = diet.nextMealWindowMinutes,
			disabledNotificationDays = diet.disabledNotificationDays,
			meals = dietMeals.map { mealDetails ->
				MealExport(
					day = mealDetails.meal.dayOfWeek,
					type = mealDetails.meal.type,
					time = "%02d:%02d".format(
						mealDetails.meal.timeMinutes / 60,
						mealDetails.meal.timeMinutes % 60,
					),
					description = mealDetails.meal.description,
					customTypeLabel = mealDetails.meal.customTypeLabel,
					isNotificationEnabled = mealDetails.meal.isNotificationEnabled,
					courses = mealDetails.courses.map { courseDetails ->
						CourseExport(
							name = courseDetails.course.name,
							order = courseDetails.course.order,
							items = courseDetails.items.map { item ->
								FoodItemExport(
									name = item.name,
									amount = item.amount,
									unit = item.unit,
									order = item.order,
								)
							},
						)
					},
				)
			},
		)
		return jsonCodec.encode(export)
	}

	override fun parseDietJson(json: String): DietExport = jsonCodec.decode(json)

	override suspend fun dietExists(uuid: String): Boolean {
		return diets.getByUuid(uuid) != null
	}

	private fun parseTime(time: String): Int {
		val parts = time.split(":")
		return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
	}

	override suspend fun importJson(json: String, overwrite: Boolean) {
		val export = parseDietJson(json)
		val existing = export.uuid?.let { diets.getByUuid(it) }

		if (existing != null && overwrite) {
			delete(existing.id)
		}

		val newDietId = create(export.name, export.nextMealWindowMinutes)
		val newDiet = diets.get(newDietId)
		if (newDiet != null) {
			diets.update(
				newDiet.copy(
					uuid = export.uuid ?: newDiet.uuid,
					disabledNotificationDays = export.disabledNotificationDays,
				),
			)
		}

		export.meals.forEach { mealExport ->
			val mealId = meals.insert(
				Meal(
					dietId = newDietId,
					type = mealExport.type,
					timeMinutes = parseTime(mealExport.time),
					dayOfWeek = mealExport.day,
					description = mealExport.description,
					customTypeLabel = mealExport.customTypeLabel,
					isNotificationEnabled = mealExport.isNotificationEnabled,
				),
			)

			mealExport.courses.forEach { courseExport ->
				val courseId = courses.insert(
					Course(
						mealId = mealId,
						name = courseExport.name,
						order = courseExport.order,
					),
				)

				courseExport.items.forEach { itemExport ->
					foodItems.insert(
						FoodItem(
							courseId = courseId,
							name = itemExport.name,
							amount = itemExport.amount,
							unit = itemExport.unit,
							order = itemExport.order,
						),
					)
				}
			}
		}
	}
}
