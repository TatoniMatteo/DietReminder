package it.matato.dietreminder.ui.screens.settings

import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.components.AppScaffold
import it.matato.dietreminder.data.model.resId
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
    section: String? = null,
    onNavigateToSettingsPage: ((String) -> Unit)? = null,
    onBack: () -> Unit = {},
    onNavigateToShopping: () -> Unit = {},
) {
    var localPage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val page = section ?: localPage
    val openPage: (String) -> Unit = { selected -> onNavigateToSettingsPage?.invoke(selected) ?: run { localPage = selected } }
    val goBack: () -> Unit = { if (section != null) onBack() else localPage = null }
    androidx.activity.compose.BackHandler(enabled = page != null, onBack = goBack)
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
        page = page, onSelectPage = openPage, onBack = goBack, onShopping = onNavigateToShopping,
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
    page: String? = null,
    onSelectPage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onShopping: () -> Unit = {},
) {
    val selected = SettingsPage.entries.find { it.key == page }
    AppScaffold(
        topBar = { if (selected != null) it.matato.dietreminder.ui.components.AppTopBar(
            title = { androidx.compose.material3.Text(stringResource(selected.title)) },
            navigationIcon = { androidx.compose.material3.IconButton(onClick = onBack) { androidx.compose.material3.Icon(it.matato.dietreminder.ui.icons.AppIcons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
        ) },
        snackbarHost = { SnackbarHost(snackBarHostState) },
    ) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpace.section),
            contentPadding = PaddingValues(horizontal = AppSpace.screen, vertical = AppSpace.group),
        ) {
            if (selected == null) {
                item { SettingsHeader() }
                item { SettingsAccountSummary { onSelectPage("account") } }
                item { SettingsHub(isDeveloperMode, onSelectPage) }
            } else when (selected) {
                SettingsPage.APPEARANCE -> item { SettingsAppearanceSection(currentTheme, dynamicColorsEnabled, seedColorHex, currentLanguage, onThemeChange, onDynamicColorsChange, onColorClick, onLanguageClick) }
                SettingsPage.ACCOUNT -> {
                    item { it.matato.dietreminder.ui.screens.account.AccountSettingsSection() }
                    item { it.matato.dietreminder.ui.screens.account.AccountSecuritySection() }
                }
                SettingsPage.PLANNING -> item { SettingsPlanningSectionContent(defaultTimes, onMealTypeClick, getFallbackTime) }
                SettingsPage.NOTIFICATIONS -> {
                    item { SettingsNotificationsSection(mealRemindersEnabled, onRemindersEnabledChange) }
                    item { SettingsShoppingNotificationsLink(onShopping) }
                }
                SettingsPage.BACKUP -> {
                    item { it.matato.dietreminder.ui.screens.account.AccountBackupSettingsSection() }
                    item { SettingsBackupSection(diets, onImportClick, onExportClick) }
                }
                SettingsPage.ABOUT -> item { SettingsAboutSection(onVersionClick) }
                SettingsPage.DEVELOPER -> item { if (isDeveloperMode) SettingsDeveloperSection(onNavigateToDeveloper) }
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
