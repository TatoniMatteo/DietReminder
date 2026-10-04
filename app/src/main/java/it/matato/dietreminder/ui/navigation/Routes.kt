package it.matato.dietreminder.ui.navigation

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
	val dayOfWeek: String,
)

@Serializable
data class DietConfigRoute(
	val dietId: Long,
)

@Serializable
data class IngredientsRoute(
	val dietId: Long? = null,
)

@Serializable
data object ShoppingListsRoute

@Serializable
data class ShoppingListDetailRoute(
	val listId: Long,
)

@Serializable
data class ImportDietFoodsRoute(
	val listId: Long,
	val dietId: Long,
)

@Serializable
data object ImportDietRoute