package it.matato.dietreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.matato.dietreminder.permission.rememberPermissionRequester
import it.matato.dietreminder.ui.navigation.AppNavigation
import it.matato.dietreminder.ui.navigation.AppNavigator
import it.matato.dietreminder.ui.navigation.RootDestination
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.util.FirstLaunchManager
import it.matato.dietreminder.viewmodel.DietViewModel

@Composable
fun App() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navigator = AppNavigator(navController)
    val vm: DietViewModel = viewModel(factory = DietViewModel.Factory)

    val requestPermissions = rememberPermissionRequester(
        context = context,
        onCompleted = {
            FirstLaunchManager.markCompleted(context)
        },
    )

    LaunchedEffect(Unit) {
        if (FirstLaunchManager.isFirstLaunch(context)) {
            requestPermissions()
        }
    }

    val useDynamicColors by vm.useDynamicColors.collectAsState()
    val seedColor by vm.seedColor.collectAsState()
    val theme by vm.theme.collectAsState()

    val isDarkTheme = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    DietTheme(
        darkTheme = isDarkTheme,
        dynamicColor = useDynamicColors,
        seedColor = seedColor.toColorOrNull(),
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        AppContent(
            currentRoute = currentRoute,
            showBottomBar = navigator.isRootDestination(currentRoute),
            onNavigateToRoot = { navigator.navigateToRoot(it) },
            content = { padding ->
                AppNavigation(
                    navController = navController,
                    vm = vm,
                    contentPadding = padding,
                )
            },
        )
    }
}

@Composable
fun AppContent(
    currentRoute: String?,
    showBottomBar: Boolean,
    onNavigateToRoot: (RootDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoot = onNavigateToRoot,
                )
            }
        },
    ) { padding ->
        content(padding)
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    onNavigateToRoot: (RootDestination) -> Unit,
) {
    NavigationBar {
        RootDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination.matches(currentRoute),
                onClick = {
                    onNavigateToRoot(destination)
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = {
                    Text(stringResource(destination.labelRes))
                },
            )
        }
    }
}

private fun String.toColorOrNull(): Color? = runCatching {
    Color(removePrefix("0x").toLong(16))
}.getOrNull()

@Preview(showBackground = true)
@Composable
private fun AppContentPreview() {
    DietTheme {
        AppContent(
            currentRoute = "WeekRoute",
            showBottomBar = true,
            onNavigateToRoot = {},
            content = { padding ->
                Text(
                    text = "App Content",
                    modifier = Modifier.padding(padding),
                )
            },
        )
    }
}