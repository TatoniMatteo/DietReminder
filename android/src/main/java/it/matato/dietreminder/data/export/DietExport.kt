package it.matato.dietreminder.data.export

import kotlinx.serialization.Serializable

@Serializable
data class DietExport(
	val formatVersion: Int = 1,
	val uuid: String? = null,
	val name: String,
	val nextMealWindowMinutes: Int = 60,
	val disabledNotificationDays: String = "",
	val meals: List<MealExport> = emptyList(),
)
