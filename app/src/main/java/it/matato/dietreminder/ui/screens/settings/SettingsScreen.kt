package it.matato.dietreminder.ui.screens.settings

import android.app.LocaleManager
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.permission.PermissionManager
import it.matato.dietreminder.ui.dialog.ColorPickerDialog
import it.matato.dietreminder.ui.dialog.ImportDialog
import it.matato.dietreminder.ui.dialog.LanguagePickerDialog
import it.matato.dietreminder.ui.dialog.TimePickerDialog
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import it.matato.dietreminder.viewmodel.ImportCheckResult
import java.io.OutputStreamWriter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: DietViewModel,
    onNavigateToDeveloper: () -> Unit = {},
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

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var dietToExport by remember { mutableStateOf<Long?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var mealTypeToEdit by remember { mutableStateOf<MealType?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var developerClickCount by remember { mutableIntStateOf(0) }

    val importSuccessMessage = stringResource(R.string.import_success)
    val exportSuccessMessage = stringResource(R.string.json_exported)
    val exportFailedMessage = stringResource(R.string.export_failed)
    val importFailedMessage = stringResource(R.string.import_failed)
    val greetingMessage = stringResource(R.string.greeting)
    val invalidImportMessage = stringResource(R.string.import_invalid_json)

    val developerClicksRemaining = stringResource(
        R.string.developer_clicks_remaining,
        7 - developerClickCount,
    )

    fun showImportError() {
        scope.launch {
            snackBarHostState.showSnackbar(invalidImportMessage)
        }
    }

    fun handleImport(json: String) {
        scope.launch {
            when (val result = vm.checkImportConflict(json)) {
                ImportCheckResult.Valid -> {
                    vm.importDiet(json).onSuccess {
                        snackBarHostState.showSnackbar(importSuccessMessage)
                    }.onFailure {
                        snackBarHostState.showSnackbar(importFailedMessage)
                    }
                }

                ImportCheckResult.Conflict -> {
                    pendingImportJson = json
                    showOverwriteDialog = true
                }

                is ImportCheckResult.Invalid -> {
                    showImportError()
                }
            }
        }
    }

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

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(selectedUri)?.use { input ->
                        val json = input.bufferedReader().use { reader ->
                            reader.readText()
                        }

                        handleImport(json)
                    }
                } catch (_: Exception) {
                    snackBarHostState.showSnackbar(importFailedMessage)
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            vm.setMealRemindersEnabled(true)
        }
    }

    fun setMealRemindersEnabled(enabled: Boolean) {
        if (!enabled) {
            vm.setMealRemindersEnabled(false)
            return
        }

        if (PermissionManager.hasNotificationPermission(context)) {
            vm.setMealRemindersEnabled(true)
        } else {
            notificationPermissionLauncher.launch(
                PermissionManager.notificationPermission(),
            )
        }
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
        onThemeChange = vm::setTheme,
        onDynamicColorsChange = vm::setUseDynamicColors,
        onColorClick = { showColorPicker = true },
        onLanguageClick = { showLanguagePicker = true },
        onMealTypeClick = { mealTypeToEdit = it },
        onRemindersEnabledChange = ::setMealRemindersEnabled,
        onImportClick = { showImportDialog = true },
        onExportClick = { dietId, fileName ->
            dietToExport = dietId
            exportLauncher.launch(fileName)
        },
        onVersionClick = {
            if (!isDeveloperMode) {
                developerClickCount++

                when {
                    developerClickCount >= 7 -> {
                        vm.setDeveloperMode(true)
                        developerClickCount = 0

                        scope.launch {
                            snackBarHostState.showSnackbar(greetingMessage)
                        }
                    }

                    developerClickCount > 3 -> {
                        scope.launch {
                            snackBarHostState.showSnackbar(developerClicksRemaining)
                        }
                    }
                }
            }
        },
        snackBarHostState = snackBarHostState,
        getFallbackTime = { vm.getDefaultFallback(it) }
    )

    if (showOverwriteDialog && pendingImportJson != null) {
        AlertDialog(
            onDismissRequest = {
                showOverwriteDialog = false
                pendingImportJson = null
            },
            title = {
                Text(stringResource(R.string.import_overwrite_title))
            },
            text = {
                Text(stringResource(R.string.import_overwrite_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportJson?.let { json ->
                            scope.launch {
                                vm.importDiet(json).onSuccess {
                                    snackBarHostState.showSnackbar(importSuccessMessage)
                                }.onFailure {
                                    snackBarHostState.showSnackbar(importFailedMessage)
                                }
                            }
                        }

                        showOverwriteDialog = false
                        pendingImportJson = null
                    },
                ) {
                    Text(stringResource(R.string.overwrite))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOverwriteDialog = false
                        pendingImportJson = null
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = {
                showImportDialog = false
            },
            onImportText = { json ->
                showImportDialog = false
                handleImport(json)
            },
            onPickFile = {
                showImportDialog = false
                importLauncher.launch(arrayOf("*/*"))
            },
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            onDismiss = {
                showColorPicker = false
            },
            onColorSelected = { color ->
                vm.setSeedColor(
                    "0x${Integer.toHexString(color.toArgb()).uppercase()}",
                )
                showColorPicker = false
            },
        )
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            onDismiss = {
                showLanguagePicker = false
            },
            onLanguageSelected = { language ->
                vm.setLanguage(language)

                context.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(language)

                showLanguagePicker = false
            },
        )
    }

    mealTypeToEdit?.let { type ->
        TimePickerDialog(
            title = stringResource(R.string.set_default_time),
            initialTimeMinutes = vm.getDefaultTime(type),
            onDismiss = {
                mealTypeToEdit = null
            },
            onTimeSelected = { timeMinutes ->
                vm.saveDefaultTime(type, timeMinutes)
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
                    getFallbackTime = getFallbackTime
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
            diets = listOf(Diet(id = 1L, name = "Summer Diet", nextMealWindowMinutes = 90, isActive = true)),
            isDeveloperMode = true,
            currentTheme = "system",
            dynamicColorsEnabled = true,
            seedColorHex = "0xFF6750A4",
            currentLanguage = "en",
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
            getFallbackTime = { 0 }
        )
    }
}