package it.matato.dietreminder.ui.navigation

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import it.matato.dietreminder.ui.screens.developer.DeveloperScreen
import it.matato.dietreminder.ui.screens.dietconfig.DietConfigScreen
import it.matato.dietreminder.ui.screens.diets.DietsScreen
import it.matato.dietreminder.ui.screens.hydration.HydrationScreen
import it.matato.dietreminder.ui.screens.mealdetail.MealDetailScreen
import it.matato.dietreminder.ui.screens.settings.SettingsScreen
import it.matato.dietreminder.ui.screens.week.WeekScreen
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel

private const val TAG = "DietNavigation"
private const val TRANSITION_DURATION = 220

class AppNavigator(
    private val navController: NavHostController,
) {
    fun navigateToRoot(destination: RootDestination) {
        Log.d(TAG, "ROOT NAVIGATION -> $destination")
        navController.navigate(destination.route()) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    fun navigate(route: Any) {
        Log.d(TAG, "NAVIGATE -> $route")
        navController.navigate(route)
    }

    fun navigateUp(): Boolean {
        Log.d(TAG, "BACK")
        return navController.navigateUp()
    }

    fun isRootDestination(route: String?): Boolean {
        return RootDestination.entries.any { it.matches(route) }
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
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_DURATION),
            ) + fadeIn(tween(TRANSITION_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_DURATION),
            ) + fadeOut(tween(TRANSITION_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_DURATION),
            ) + fadeIn(tween(TRANSITION_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_DURATION),
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
            )
        }

        composable<HydrationRoute>(
            deepLinks = listOf(
                navDeepLink<HydrationRoute>(
                    basePath = "dietreminder://hydration",
                ),
            ),
        ) {
            HydrationScreen(vm = vm)
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
                onNavigateToDeveloper = {
                    navigator.navigate(DeveloperRoute)
                },
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
                onBack = navigator::navigateUp,
                onAddMeal = { mealId, dayOfWeek ->
                    navigator.navigate(
                        MealDetailRoute(
                            mealId = mealId,
                            dietId = route.dietId,
                            dayOfWeek = dayOfWeek,
                        ),
                    )
                },
                onEditMeal = { mealId, dayOfWeek ->
                    navigator.navigate(
                        MealDetailRoute(
                            mealId = mealId,
                            dietId = route.dietId,
                            dayOfWeek = dayOfWeek,
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
                dayOfWeek = route.dayOfWeek,
                onBack = navigator::navigateUp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppNavigationPreview() {
    DietTheme {
        Text("App Navigation Host")
    }
}