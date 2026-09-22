package it.matato.dietreminder.ui.navigation

import java.time.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
data class WeekRoute(
    val mealId: Long? = null,
    val dayName: String? = null,
)

@Serializable
data object DietsRoute

@Serializable
data object HydrationRoute

@Serializable
data object SettingsRoute

@Serializable
data object DeveloperRoute

@Serializable
data class MealDetailRoute(
    val mealId: Long,
    val dietId: Long,
    val dayOfWeek: DayOfWeek,
)

@Serializable
data class DietConfigRoute(
    val dietId: Long,
)