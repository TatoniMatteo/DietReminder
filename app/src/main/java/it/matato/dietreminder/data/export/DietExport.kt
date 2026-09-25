package it.matato.dietreminder.data.export

import kotlinx.serialization.Serializable

@Serializable
data class DietExport(
    val uuid: String? = null,
    val name: String,
    val nextMealWindowMinutes: Int,
    val disabledNotificationDays: String = "",
    val meals: List<MealExport>
)
