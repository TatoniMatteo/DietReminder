package it.matato.dietreminder.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import it.matato.dietreminder.ui.theme.AppMotion
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import it.matato.dietreminder.ui.screens.developer.DeveloperScreen
import it.matato.dietreminder.ui.screens.dietconfig.DietConfigScreen
import it.matato.dietreminder.ui.screens.diets.DietsScreen
import it.matato.dietreminder.ui.screens.importdiet.ImportDietScreen
import it.matato.dietreminder.ui.screens.ingredients.IngredientsScreen
import it.matato.dietreminder.ui.screens.mealdetail.MealDetailScreen
import it.matato.dietreminder.ui.screens.settings.SettingsScreen
import it.matato.dietreminder.ui.screens.shoppinglist.ImportDietFoodsScreen
import it.matato.dietreminder.ui.screens.shoppinglist.ShoppingListDetailScreen
import it.matato.dietreminder.ui.screens.shoppinglist.ShoppingListsScreen
import it.matato.dietreminder.ui.screens.week.WeekScreen
import it.matato.dietreminder.viewmodel.DietViewModel

const val TRANSITION_DURATION = AppMotion.standard

class AppNavigator(val navController: NavHostController) {

    fun navigateToRoot(destination: RootDestination) {
        val reselecting = destination.matches(navController.currentDestination?.route)
        // Reselecting the current section returns to its root rather than restoring its detail.
        if (reselecting && navController.popBackStack(destination.route(), inclusive = false)) return
        navController.navigate(destination.route()) {
			popUpTo(navController.graph.startDestinationId) {
				saveState = true
			}

			launchSingleTop = true
            restoreState = !reselecting
		}
	}

	fun navigate(route: Any) {
		navController.navigate(route)
	}

	fun navigateUp(): Boolean {
		return navController.navigateUp()
	}

	fun isRootDestination(route: String?): Boolean {
		return route?.contains("SettingsDetailRoute") != true && RootDestination.entries.any { it.matches(route) }
	}
}

@Composable
fun AppNavigation(
	navController: NavHostController,
	vm: DietViewModel,
	contentPadding: PaddingValues = PaddingValues(),
) {
	val navigator = AppNavigator(navController)

	NavHost(
		modifier = Modifier.padding(contentPadding),
		navController = navController,
		startDestination = WeekRoute(),
		enterTransition = {
            if (navigator.isRootDestination(initialState.destination.route) && navigator.isRootDestination(targetState.destination.route)) {
                fadeIn(tween(AppMotion.standard))
            } else slideIntoContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.Start,
				animationSpec = tween(TRANSITION_DURATION),
                initialOffset = { it / 8 },
			) + fadeIn(tween(TRANSITION_DURATION))
		},
		exitTransition = {
            if (navigator.isRootDestination(initialState.destination.route) && navigator.isRootDestination(targetState.destination.route)) {
                fadeOut(tween(AppMotion.quick))
            } else slideOutOfContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.Start,
				animationSpec = tween(TRANSITION_DURATION),
                targetOffset = { it / 8 },
			) + fadeOut(tween(TRANSITION_DURATION))
		},
		popEnterTransition = {
            if (navigator.isRootDestination(initialState.destination.route) && navigator.isRootDestination(targetState.destination.route)) {
                fadeIn(tween(AppMotion.standard))
            } else slideIntoContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.End,
				animationSpec = tween(TRANSITION_DURATION),
                initialOffset = { it / 8 },
			) + fadeIn(tween(TRANSITION_DURATION))
		},
		popExitTransition = {
            if (navigator.isRootDestination(initialState.destination.route) && navigator.isRootDestination(targetState.destination.route)) {
                fadeOut(tween(AppMotion.quick))
            } else slideOutOfContainer(
				towards = AnimatedContentTransitionScope.SlideDirection.End,
				animationSpec = tween(TRANSITION_DURATION),
                targetOffset = { it / 8 },
			) + fadeOut(tween(TRANSITION_DURATION))
		},
	) {
		composable<WeekRoute>(
			deepLinks = listOf(
				navDeepLink<WeekRoute>(
					basePath = "dietreminder://week",
				),
			),
		) {
			val route = it.toRoute<WeekRoute>()

			WeekScreen(
				vm = vm,
				targetMealId = route.mealId,
				targetDayName = route.dayName,
                onOpenDiet = { dietId ->
                    if (dietId == null) navigator.navigateToRoot(RootDestination.DIETS)
                    else navigator.navigate(DietConfigRoute(dietId))
                },
                onEditMeal = { details ->
                    navigator.navigate(MealDetailRoute(details.meal.id, details.meal.dietId, details.meal.dayOfWeek.name))
                },
			)
		}

		composable<DietsRoute>(
			deepLinks = listOf(
				navDeepLink<DietsRoute>(
					basePath = "dietreminder://diets",
				),
			),
		) {
			DietsScreen(
				vm = vm,
				onConfigDiet = { dietId ->
					navigator.navigate(
						DietConfigRoute(dietId),
					)
				},
				onNavigateToImportDiet = {
					navigator.navigate(ImportDietRoute)
				},
			)
		}

		composable<SettingsRoute>(
			deepLinks = listOf(
				navDeepLink<SettingsRoute>(
					basePath = "dietreminder://settings",
				),
			),
		) {
			SettingsScreen(
				vm = vm,
                onNavigateToSettingsPage = { navigator.navigate(SettingsDetailRoute(it)) },
                onNavigateToShopping = { navigator.navigate(ShoppingListsRoute) },
				onNavigateToDeveloper = {
					navigator.navigate(DeveloperRoute)
				},
				onNavigateToImportDiet = {
					navigator.navigate(ImportDietRoute)
				},
			)
		}

        composable<SettingsDetailRoute> {
            val route = it.toRoute<SettingsDetailRoute>()
            SettingsScreen(vm = vm, section = route.section,
                onNavigateToSettingsPage = { navigator.navigate(SettingsDetailRoute(it)) },
                onBack = { navigator.navigateUp() },
                onNavigateToDeveloper = { navigator.navigate(DeveloperRoute) },
                onNavigateToImportDiet = { navigator.navigate(ImportDietRoute) },
                onNavigateToShopping = { navigator.navigate(ShoppingListsRoute) },
            )
        }

		composable<DeveloperRoute> {
			DeveloperScreen(
				vm = vm,
				onBack = navigator::navigateUp,
			)
		}

		composable<DietConfigRoute> {
			val route = it.toRoute<DietConfigRoute>()

			DietConfigScreen(
				vm = vm,
				dietId = route.dietId,
				onBack = {
					navigator.navigateUp()
				},
				onIngredientsClick = {
					navigator.navigate(IngredientsRoute(route.dietId))
				},
				onAddMeal = { dietId, dayOfWeek ->
					navigator.navigate(
						MealDetailRoute(
							mealId = 0L,
							dietId = dietId,
							dayOfWeek = dayOfWeek.name,
						),
					)
				},
				onEditMeal = { mealId, dayOfWeek ->
					navigator.navigate(
						MealDetailRoute(
							mealId = mealId,
							dietId = route.dietId,
							dayOfWeek = dayOfWeek.name,
						),
					)
				},
			)
		}

		composable<MealDetailRoute> {
			val route = it.toRoute<MealDetailRoute>()

			MealDetailScreen(
				vm = vm,
				mealId = route.mealId,
				dietId = route.dietId,
				dayOfWeek = java.time.DayOfWeek.valueOf(route.dayOfWeek),
				onBack = {
					navigator.navigateUp()
				},
			)
		}

		composable<IngredientsRoute>(
			deepLinks = listOf(
				navDeepLink<IngredientsRoute>(
					basePath = "dietreminder://ingredients",
				),
			),
		) {
			val route = it.toRoute<IngredientsRoute>()

			IngredientsScreen(
				vm = vm,
				dietId = route.dietId,
				onBack = { navigator.navigateUp() },
			)
		}

		composable<ShoppingListsRoute>(
			deepLinks = listOf(
				navDeepLink<ShoppingListsRoute>(
					basePath = "dietreminder://shoppinglists",
				),
			),
		) {
			ShoppingListsScreen(
				vm = vm,
				onBack = null,
				onSelectList = { listId ->
					navigator.navigate(ShoppingListDetailRoute(listId))
				},
				onNavigateToImportDietFoods = { listId, dietId ->
					navigator.navigate(ImportDietFoodsRoute(listId, dietId))
				},
				onNavigateToIngredients = {
					navigator.navigate(IngredientsRoute())
				},
			)
		}

		composable<ShoppingListDetailRoute>(deepLinks = listOf(navDeepLink<ShoppingListDetailRoute>(basePath = "dietreminder://shopping"))) {
			val route = it.toRoute<ShoppingListDetailRoute>()

			ShoppingListDetailScreen(
				vm = vm,
				listId = route.listId,
				onBack = { navigator.navigateUp() },
				onNavigateToImportDietFoods = { listId, dietId ->
					navigator.navigate(ImportDietFoodsRoute(listId, dietId))
				},
			)
		}

		composable<ImportDietFoodsRoute> {
			val route = it.toRoute<ImportDietFoodsRoute>()

			ImportDietFoodsScreen(
				vm = vm,
				listId = route.listId,
				dietId = route.dietId,
				onBack = { navigator.navigateUp() },
			)
		}

		composable<ImportDietRoute> {
			ImportDietScreen(
				vm = vm,
				onBack = { navigator.navigateUp() },
				onImportSuccess = { navigator.navigateUp() },
			)
		}
	}
}

@Composable
private fun AppNavigationPreview() {
	val navController = rememberNavController()

	NavHost(
		navController = navController,
		startDestination = "WeekRoute",
	) {
	}
}
