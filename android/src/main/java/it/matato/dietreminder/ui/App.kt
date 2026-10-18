package it.matato.dietreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.AppVersionState
import it.matato.dietreminder.permission.rememberPermissionRequester
import it.matato.dietreminder.ui.dialog.UpdateDialog
import it.matato.dietreminder.ui.navigation.AppNavigation
import it.matato.dietreminder.ui.navigation.AppNavigator
import it.matato.dietreminder.ui.navigation.RootDestination
import it.matato.dietreminder.ui.screens.update.ObsoleteScreen
import it.matato.dietreminder.ui.screens.update.OfflineScreen
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.util.FirstLaunchManager
import it.matato.dietreminder.viewmodel.DietViewModel

@Composable
fun App() {
	val context = LocalContext.current
	val navController = rememberNavController()
	val navigator = AppNavigator(navController)
	val vm: DietViewModel = hiltViewModel()

	val requestPermissions = rememberPermissionRequester(context) {
		FirstLaunchManager.markCompleted(context)
	}

	LaunchedEffect(Unit) {
		if (FirstLaunchManager.isFirstLaunch(context)) {
			requestPermissions()
		}
	}

	val useDynamicColors by vm.useDynamicColors.collectAsState()
	val seedColor by vm.seedColor.collectAsState()
	val theme by vm.theme.collectAsState()
	val versionStatus by vm.versionStatus.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }
	val writeBlockedMessage = stringResource(R.string.offline_write_blocked)
	val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
	var showUpdateDialog by rememberSaveable(versionStatus.state) { mutableStateOf(true) }
	var continueOffline by rememberSaveable { mutableStateOf(false) }

	LaunchedEffect(versionStatus.isOffline) {
		if (!versionStatus.isOffline) {
			continueOffline = false
		}
	}

	LaunchedEffect(vm, writeBlockedMessage) {
		vm.writeBlockedEvents.collect {
			snackbarHostState.showSnackbar(writeBlockedMessage)
		}
	}

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
		if (versionStatus.state == AppVersionState.OBSOLETE) {
			ObsoleteScreen(
				onUpdateClick = { uriHandler.openUri(versionStatus.updateUrl) },
			)
		} else if (versionStatus.isOffline && !continueOffline) {
			OfflineScreen(
				onRetryClick = vm::checkUpdate,
				onContinueClick = { continueOffline = true },
			)
		} else {
			if (showUpdateDialog && (versionStatus.state == AppVersionState.RECENT || versionStatus.state == AppVersionState.DEPRECATED)) {
				UpdateDialog(
					state = versionStatus.state,
					onUpdateClick = { uriHandler.openUri(versionStatus.updateUrl) },
					onDismiss = { showUpdateDialog = false },
				)
			}

			val backStackEntry by navController.currentBackStackEntryAsState()
			val currentRoute = backStackEntry?.destination?.route

			androidx.compose.runtime.CompositionLocalProvider(
				LocalOfflineMode provides (versionStatus.isOffline || versionStatus.isChecking),
			) {
				AppContent(
					currentRoute = currentRoute,
					showBottomBar = navigator.isRootDestination(currentRoute),
					isOffline = versionStatus.isOffline,
					snackbarHostState = snackbarHostState,
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
	}
}

@Composable
fun AppContent(
	currentRoute: String?,
	showBottomBar: Boolean,
	isOffline: Boolean,
	snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
	onNavigateToRoot: (RootDestination) -> Unit,
	content: @Composable (PaddingValues) -> Unit,
) {
	androidx.compose.material3.Scaffold(
		topBar = {
			if (isOffline) {
				androidx.compose.material3.Surface(
					color = MaterialTheme.colorScheme.errorContainer,
					modifier = androidx.compose.ui.Modifier
						.fillMaxWidth()
						.windowInsetsPadding(WindowInsets.statusBars),
				) {
					androidx.compose.foundation.layout.Row(
						modifier = androidx.compose.ui.Modifier
							.fillMaxWidth()
							.padding(8.dp),
						horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
						verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
					) {
						Text(
							text = stringResource(R.string.offline_mode_banner),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onErrorContainer,
							fontWeight = FontWeight.Bold,
						)
					}
				}
			}
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
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
	NavigationBar(
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		tonalElevation = 6.dp,
	) {
		RootDestination.entries.forEach { destination ->
			val selected = destination.matches(currentRoute)
			NavigationBarItem(
				selected = selected,
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
					Text(
						text = stringResource(destination.labelRes),
						textAlign = TextAlign.Center,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
					)
				},
				alwaysShowLabel = true,
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
			isOffline = false,
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
