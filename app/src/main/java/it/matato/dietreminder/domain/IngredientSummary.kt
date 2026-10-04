package it.matato.dietreminder.domain

import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.QuantityUnit
import java.time.DayOfWeek

data class IngredientOccurrence(
	val dayOfWeek: DayOfWeek,
	val mealType: MealType,
	val mealCustomLabel: String? = null,
	val courseName: String = "",
	val amount: String = "",
	val unit: QuantityUnit = QuantityUnit.GRAMS,
) {
	val displayQuantity: String
		get() = unit.format(amount)

	val quantity: String
		get() = displayQuantity
}

data class IngredientSummary(
	val name: String,
	val occurrences: List<IngredientOccurrence>,
) {
	val totalOccurrences: Int get() = occurrences.size

	val dailyQuantities: Map<DayOfWeek, List<String>>
		get() {
			return occurrences.groupBy { it.dayOfWeek }
				.mapValues { (_, occurrencesList) -> occurrencesList.map { it.displayQuantity } }
				.toSortedMap(compareBy { it.ordinal })
		}
}

fun List<MealWithDetails>.toIngredientSummaries(): List<IngredientSummary> {
	val itemsWithDetails = this.flatMap { mealWithDetails ->
		val meal = mealWithDetails.meal
		mealWithDetails.courses.flatMap { courseWithItems ->
			courseWithItems.items.map { foodItem ->
				Triple(meal, courseWithItems.course, foodItem)
			}
		}
	}

	val grouped = itemsWithDetails.groupBy { (_, _, item) ->
		item.name.trim().lowercase()
	}

	return grouped.mapNotNull { (key, entries) ->
		if (key.isBlank()) return@mapNotNull null

		val bestName = entries.firstOrNull { it.third.name.isNotBlank() }?.third?.name?.trim()
			?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
			?: key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

		val occurrences = entries.map { (meal, course, item) ->
			IngredientOccurrence(
				dayOfWeek = meal.dayOfWeek,
				mealType = meal.type,
				mealCustomLabel = meal.customTypeLabel,
				courseName = course.name.trim(),
				amount = item.amount.trim(),
				unit = item.unit,
			)
		}.sortedWith(compareBy({ it.dayOfWeek.ordinal }, { it.mealType.ordinal }))

		IngredientSummary(
			name = bestName,
			occurrences = occurrences,
		)
	}.sortedBy { it.name }
}
