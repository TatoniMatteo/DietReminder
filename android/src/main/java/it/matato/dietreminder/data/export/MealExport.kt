package it.matato.dietreminder.data.export

import it.matato.dietreminder.data.model.MealType
import java.time.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
data class MealExport(
    val day: DayOfWeek,
    val type: MealType,
    val time: String,
    val description: String = "",
    val customTypeLabel: String? = null,
    val isNotificationEnabled: Boolean = true,
    val courses: List<CourseExport> = emptyList()
)
