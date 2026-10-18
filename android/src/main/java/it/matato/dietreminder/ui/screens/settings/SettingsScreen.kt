package it.matato.dietreminder.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.dialog.ColorPickerDialog
import it.matato.dietreminder.ui.dialog.LanguagePickerDialog
import it.matato.dietreminder.ui.dialog.TimePickerDialog
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.io.OutputStreamWriter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
	vm: DietViewModel,
	onNavigateToDeveloper: () -> Unit = {},
	onNavigateToImportDiet: () -> Unit = {},
) {
	val context = LocalContext.current
	val diets by vm.diets.collectAsState()
	val isDeveloperMode by vm.isDeveloperMode.collectAsState()
	val currentTheme by vm.theme.collectAsState()
	val dynamicColorsEnabled by vm.useDynamicColors.collectAsState()
	val seedColorHex by vm.seedColor.collectAsState()
	val currentLanguage by vm.language.collectAsState()
	val defaultTimes by vm.defaultTimes.collectAsState()
	val mealRemindersEnabled by vm.mealRemindersEnabled.collectAsState()

	val scope = rememberCoroutineScope()
	val snackBarHostState = remember { SnackbarHostState() }

	var dietToExport by remember { mutableStateOf<Long?>(null) }
	var mealTypeToEdit by remember { mutableStateOf<MealType?>(null) }
	var showColorPicker by remember { mutableStateOf(false) }
	var showLanguagePicker by remember { mutableStateOf(false) }

	var developerClickCount by remember { mutableIntStateOf(0) }

	val exportSuccessMessage = stringResource(R.string.json_exported)
	val exportFailedMessage = stringResource(R.string.export_failed)
	val developerModeActivatedMessage = stringResource(R.string.developer_mode_activated)
	val developerClicksRemaining = pluralStringResource(
		R.plurals.developer_clicks_remaining,
		7 - developerClickCount,
		7 - developerClickCount,
	)

	val exportLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.CreateDocument("application/vnd.matato.dietreminder"),
	) { uri ->
		uri?.let { selectedUri ->
			dietToExport?.let { dietId ->
				scope.launch {
					try {
						val json = vm.exportDiet(dietId)

						context.contentResolver.openOutputStream(selectedUri)?.use { output ->
							OutputStreamWriter(output).use { writer ->
								writer.write(json)
							}
						}

						snackBarHostState.showSnackbar(exportSuccessMessage)
					} catch (_: Exception) {
						snackBarHostState.showSnackbar(exportFailedMessage)
					}
				}
			}
		}

		dietToExport = null
	}

	SettingsContent(
		diets = diets,
		isDeveloperMode = isDeveloperMode,
		currentTheme = currentTheme,
		dynamicColorsEnabled = dynamicColorsEnabled,
		seedColorHex = seedColorHex,
		currentLanguage = currentLanguage,
		defaultTimes = defaultTimes,
		mealRemindersEnabled = mealRemindersEnabled,
		onNavigateToDeveloper = onNavigateToDeveloper,
		onThemeChange = { theme -> vm.setTheme(theme) },
		onDynamicColorsChange = { enabled -> vm.setUseDynamicColors(enabled) },
		onColorClick = { showColorPicker = true },
		onLanguageClick = { showLanguagePicker = true },
		onMealTypeClick = { type -> mealTypeToEdit = type },
		onRemindersEnabledChange = { enabled -> vm.setMealRemindersEnabled(enabled) },
		onImportClick = onNavigateToImportDiet,
		onExportClick = { dietId, fileName ->
			dietToExport = dietId
			exportLauncher.launch(fileName)
		},
		onVersionClick = {
			if (!isDeveloperMode) {
				developerClickCount++

				if (developerClickCount >= 7) {
					vm.setDeveloperMode(true)
					developerClickCount = 0

					scope.launch {
						snackBarHostState.showSnackbar(developerModeActivatedMessage)
					}
				} else if (developerClickCount >= 3) {
					scope.launch {
						snackBarHostState.showSnackbar(developerClicksRemaining)
					}
				}
			}
		},
		snackBarHostState = snackBarHostState,
		getFallbackTime = { vm.getDefaultFallback(it) },
	)

	if (showColorPicker) {
		ColorPickerDialog(
			onDismiss = { showColorPicker = false },
			onColorSelected = { color ->
				val colorHex = "0x" + color.toArgb().toUInt().toString(16)
				vm.setSeedColor(colorHex)
				showColorPicker = false
			},
		)
	}

	if (showLanguagePicker) {
		LanguagePickerDialog(
			onDismiss = { showLanguagePicker = false },
			onLanguageSelected = { languageCode ->
				vm.setLanguage(languageCode)
				showLanguagePicker = false
			},
		)
	}

	if (mealTypeToEdit != null) {
		val currentMealType = mealTypeToEdit!!
		val currentTime = defaultTimes.find { it.type == currentMealType }?.timeMinutes
			?: vm.getDefaultFallback(currentMealType)

		TimePickerDialog(
			title = stringResource(currentMealType.resId),
			initialTimeMinutes = currentTime,
			onDismiss = { mealTypeToEdit = null },
			onTimeSelected = { timeMinutes ->
				vm.saveDefaultTime(currentMealType, timeMinutes)
				mealTypeToEdit = null
			},
		)
	}
}

@Composable
fun SettingsContent(
	diets: List<Diet>,
	isDeveloperMode: Boolean,
	currentTheme: String,
	dynamicColorsEnabled: Boolean,
	seedColorHex: String,
	currentLanguage: String,
	defaultTimes: List<MealDefaultTime>,
	mealRemindersEnabled: Boolean,
	onNavigateToDeveloper: () -> Unit,
	onThemeChange: (String) -> Unit,
	onDynamicColorsChange: (Boolean) -> Unit,
	onColorClick: () -> Unit,
	onLanguageClick: () -> Unit,
	onMealTypeClick: (MealType) -> Unit,
	onRemindersEnabledChange: (Boolean) -> Unit,
	onImportClick: () -> Unit,
	onExportClick: (Long, String) -> Unit,
	onVersionClick: () -> Unit,
	snackBarHostState: SnackbarHostState,
	getFallbackTime: (MealType) -> Int,
) {
	Scaffold(
		snackbarHost = {
			SnackbarHost(snackBarHostState)
		},
	) { innerPadding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding),
			verticalArrangement = Arrangement.spacedBy(24.dp),
			contentPadding = PaddingValues(
				horizontal = 20.dp,
				vertical = 24.dp,
			),
		) {
			item {
				SettingsHeader()
			}

			item {
				SettingsAppearanceSection(
					currentTheme = currentTheme,
					dynamicEnabled = dynamicColorsEnabled,
					seedColorHex = seedColorHex,
					currentLanguage = currentLanguage,
					onThemeChange = onThemeChange,
					onDynamicColorsChange = onDynamicColorsChange,
					onColorClick = onColorClick,
					onLanguageClick = onLanguageClick,
				)
			}

			item {
				SettingsPlanningSectionContent(
					defaultTimes = defaultTimes,
					onMealTypeClick = onMealTypeClick,
					getFallbackTime = getFallbackTime,
				)
			}

			item {
				SettingsNotificationsSection(
					enabled = mealRemindersEnabled,
					onEnabledChange = onRemindersEnabledChange,
				)
			}

			item {
				SettingsBackupSection(
					diets = diets,
					onImport = onImportClick,
					onExport = onExportClick,
				)
			}

			if (isDeveloperMode) {
				item {
					SettingsDeveloperSection(
						onNavigateToDeveloper = onNavigateToDeveloper,
					)
				}
			}

			item {
				SettingsAboutSection(
					onVersionClick = onVersionClick,
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
	DietTheme {
		SettingsContent(
			diets = emptyList(),
			isDeveloperMode = true,
			currentTheme = "system",
			dynamicColorsEnabled = true,
			seedColorHex = "0xFF6750A4",
			currentLanguage = "it",
			defaultTimes = emptyList(),
			mealRemindersEnabled = true,
			onNavigateToDeveloper = {},
			onThemeChange = {},
			onDynamicColorsChange = {},
			onColorClick = {},
			onLanguageClick = {},
			onMealTypeClick = {},
			onRemindersEnabledChange = {},
			onImportClick = {},
			onExportClick = { _, _ -> },
			onVersionClick = {},
			snackBarHostState = SnackbarHostState(),
			getFallbackTime = { 480 },
		)
	}
}
