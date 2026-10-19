package it.matato.dietreminder.ui.navigation

import androidx.annotation.StringRes
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.ui.graphics.vector.ImageVector
import it.matato.dietreminder.R

enum class RootDestination(
	val icon: ImageVector,
	@StringRes val labelRes: Int,
) {
	WEEK(
		icon = Icons.Rounded.CalendarMonth,
		labelRes = R.string.week,
	),
	DIETS(
		icon = Icons.Rounded.Restaurant,
		labelRes = R.string.diets,
	),
	SHOPPING_LISTS(
		icon = Icons.Rounded.ShoppingCart,
		labelRes = R.string.nav_shopping_lists,
	),
	SETTINGS(
		icon = Icons.Rounded.Settings,
		labelRes = R.string.settings,
	);

	fun route(): Any {
		return when (this) {
			WEEK -> WeekRoute()
			DIETS -> DietsRoute
			SHOPPING_LISTS -> ShoppingListsRoute
			SETTINGS -> SettingsRoute
		}
	}

	fun matches(route: String?): Boolean {
		return when (this) {
			WEEK -> route?.contains("WeekRoute") == true
			DIETS -> route?.contains("DietsRoute") == true
			SHOPPING_LISTS -> (route?.contains("ShoppingListsRoute") == true) || (route?.contains("ShoppingListDetailRoute") == true) || (route?.contains(
				"ImportDietFoodsRoute") == true) || (route?.contains("IngredientsRoute") == true)

			SETTINGS -> route?.contains("SettingsRoute") == true || route?.contains("SettingsDetailRoute") == true
		}
	}
}
