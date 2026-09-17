package it.matato.dietreminder.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.screens.developer.DeveloperScreen
import it.matato.dietreminder.ui.screens.dietconfig.DietConfigScreen
import it.matato.dietreminder.ui.screens.diets.DietsScreen
import it.matato.dietreminder.ui.screens.hydration.HydrationScreen
import it.matato.dietreminder.ui.screens.mealdetail.MealDetailScreen
import it.matato.dietreminder.ui.screens.settings.SettingsScreen
import it.matato.dietreminder.ui.screens.week.WeekScreen
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import it.matato.dietreminder.util.AppLog
import java.time.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
data class WeekRoute(
    val mealId: Long? = null,
    val dayName: String? = null
)

@Serializable
object DietsRoute

@Serializable
object HydrationRoute

@Serializable
object SettingsRoute

@Serializable
object DeveloperRoute

@Serializable
data class MealDetailRoute(
    val mealId: Long,
    val dietId: Long,
    val dayOfWeek: String
)

@Serializable
data class DietConfigRoute(
    val dietId: Long
)

@Composable
fun App(vm: DietViewModel = viewModel()) {
    val context = LocalContext.current
    val theme by vm.theme.collectAsState()
    val dynamicEnabled by vm.useDynamicColors.collectAsState()
    val seedColorHex by vm.seedColor.collectAsState()

    val seedColor = Color(
        seedColorHex
            .removePrefix("0x")
            .toLong(16)
    )

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            AppLog.d("POST_NOTIFICATIONS permission not granted. Alarms won't show notifications.")
        }
    }

    val isDark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    DietTheme(
        darkTheme = isDark,
        dynamicColor = dynamicEnabled,
        seedColor = if (dynamicEnabled) null else seedColor
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val isRootDestination =
            currentDestination?.hierarchy?.any {
                it.hasRoute(WeekRoute::class) ||
                        it.hasRoute(DietsRoute::class) ||
                        it.hasRoute(HydrationRoute::class) ||
                        it.hasRoute(SettingsRoute::class)
            } == true

        BackHandler(enabled = isRootDestination) {
            (context as? Activity)?.finish()
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (isRootDestination) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        val items = listOf(
                            Triple(
                                WeekRoute(),
                                Icons.Rounded.CalendarMonth,
                                stringResource(R.string.week)
                            ),
                            Triple(
                                DietsRoute,
                                Icons.Rounded.RestaurantMenu,
                                stringResource(R.string.diets)
                            ),
                            Triple(
                                HydrationRoute,
                                Icons.Rounded.WaterDrop,
                                stringResource(R.string.hydration_title)
                            ),
                            Triple(
                                SettingsRoute,
                                Icons.Rounded.Settings,
                                stringResource(R.string.settings)
                            )
                        )

                        items.forEach { (route, icon, label) ->
                            val isSelected = currentDestination.hierarchy.any {
                                it.hasRoute(route::class)
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(label)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = WeekRoute(),
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(400)
                    ) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(400)
                    )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(400)
                    ) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(400)
                    )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(400)
                    ) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400)
                    )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(400)
                    ) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400)
                    )
                }
            ) {
                composable<WeekRoute>(
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern =
                                "dietreminder://week?mealId={mealId}&dayName={dayName}"
                        }
                    )
                ) { backStackEntry ->
                    val route: WeekRoute = backStackEntry.toRoute()

                    WeekScreen(
                        vm = vm,
                        padding = padding,
                        targetMealId = route.mealId,
                        targetDayName = route.dayName
                    )
                }

                composable<DietsRoute>(
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "dietreminder://diets"
                        }
                    )
                ) {
                    DietsScreen(
                        vm = vm,
                        padding = padding,
                        onConfigDiet = { dietId ->
                            navController.navigate(
                                DietConfigRoute(dietId)
                            )
                        }
                    )
                }

                composable<HydrationRoute>(
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "dietreminder://hydration"
                        }
                    )
                ) {
                    HydrationScreen(
                        vm = vm,
                        padding = padding
                    )
                }

                composable<SettingsRoute>(
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "dietreminder://settings"
                        }
                    )
                ) {
                    SettingsScreen(
                        vm = vm,
                        padding = padding,
                        onNavigateToDeveloper = {
                            navController.navigate(DeveloperRoute)
                        }
                    )
                }

                composable<DeveloperRoute> {
                    DeveloperScreen(
                        vm = vm,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<DietConfigRoute> { backStackEntry ->
                    val route: DietConfigRoute = backStackEntry.toRoute()

                    DietConfigScreen(
                        vm = vm,
                        dietId = route.dietId,
                        onBack = {
                            navController.popBackStack()
                        },
                        onAddMeal = { dietId, day ->
                            navController.navigate(
                                MealDetailRoute(
                                    mealId = 0L,
                                    dietId = dietId,
                                    dayOfWeek = day.name
                                )
                            )
                        },
                        onEditMeal = { mealId, dietId ->
                            navController.navigate(
                                MealDetailRoute(
                                    mealId = mealId,
                                    dietId = dietId,
                                    dayOfWeek = DayOfWeek.MONDAY.name
                                )
                            )
                        }
                    )
                }

                composable<MealDetailRoute> { backStackEntry ->
                    val route: MealDetailRoute = backStackEntry.toRoute()

                    MealDetailScreen(
                        vm = vm,
                        mealId = route.mealId,
                        dietId = route.dietId,
                        dayOfWeek = DayOfWeek.valueOf(route.dayOfWeek),
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}