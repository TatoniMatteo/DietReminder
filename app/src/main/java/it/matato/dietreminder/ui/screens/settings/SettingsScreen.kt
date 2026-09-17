package it.matato.dietreminder.ui.screens.settings

import android.Manifest
import android.app.LocaleManager
import android.content.pm.PackageManager
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import it.matato.dietreminder.R
import it.matato.dietreminder.data.MealType
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import java.io.OutputStreamWriter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: DietViewModel,
    padding: PaddingValues,
    onNavigateToDeveloper: () -> Unit = {}
) {
    val context = LocalContext.current
    val diets by vm.diets.collectAsState()
    val isDevMode by vm.isDeveloperMode.collectAsState()
    val currentTheme by vm.theme.collectAsState()
    val dynamicEnabled by vm.useDynamicColors.collectAsState()
    val seedColorHex by vm.seedColor.collectAsState()
    val currentLang by vm.language.collectAsState()
    val defaultTimesState by vm.defaultTimes.collectAsState()
    val mealRemindersEnabled by vm.mealRemindersEnabled.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val importSuccessMessage = stringResource(R.string.import_success)
    val exportSuccessMessage = stringResource(R.string.json_exported)
    val exportFailedMessage = stringResource(R.string.export_failed)
    val importFailedMessage = stringResource(R.string.import_failed)
    val greetingMessage = stringResource(R.string.greeting)

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var dietToExport by remember { mutableStateOf<Long?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var mealTypeToEdit by remember { mutableStateOf<MealType?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var devClickCount by remember { mutableIntStateOf(0) }

    val handleImport = { json: String ->
        scope.launch {
            if (vm.checkImportConflict(json)) {
                pendingImportJson = json
                showOverwriteDialog = true
            } else {
                vm.importDiet(json)
                snackbarHostState.showSnackbar(importSuccessMessage)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            dietToExport?.let { id ->
                scope.launch {
                    try {
                        val json = vm.exportDiet(id)

                        context.contentResolver.openOutputStream(it)?.use { output ->
                            OutputStreamWriter(output).use { writer ->
                                writer.write(json)
                            }
                        }

                        snackbarHostState.showSnackbar(exportSuccessMessage)
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar(exportFailedMessage)
                    }
                }
            }
        }

        dietToExport = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { input ->
                        val json = input.bufferedReader().use { reader ->
                            reader.readText()
                        }

                        handleImport(json)
                    }
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar(importFailedMessage)
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            vm.syncAlarms()
        }
    }

    fun requestNotificationPermission(): Boolean {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
            return false
        }

        return true
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        modifier = Modifier.padding(padding)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 24.dp
            )
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.appearance_personalization),
                    icon = Icons.Rounded.Tune
                ) {
                    SettingsListItem(
                        title = stringResource(R.string.theme),
                        subtitle = when (currentTheme) {
                            "light" -> stringResource(R.string.theme_light)
                            "dark" -> stringResource(R.string.theme_dark)
                            else -> stringResource(R.string.theme_system)
                        },
                        leadingIcon = Icons.Rounded.Brightness4,
                        onClick = {
                            val next = when (currentTheme) {
                                "system" -> "light"
                                "light" -> "dark"
                                else -> "system"
                            }

                            vm.setTheme(next)
                        }
                    )

                    SettingsDivider()

                    SettingsListItem(
                        title = stringResource(R.string.dynamic_colors),
                        subtitle = stringResource(R.string.dynamic_colors_desc),
                        leadingIcon = Icons.Rounded.ColorLens,
                        trailingContent = {
                            Switch(
                                checked = dynamicEnabled,
                                onCheckedChange = vm::setUseDynamicColors
                            )
                        },
                        onClick = {
                            vm.setUseDynamicColors(!dynamicEnabled)
                        }
                    )

                    SettingsDivider()

                    val seedColor = Color(
                        seedColorHex
                            .removePrefix("0x")
                            .toLong(16)
                    )

                    SettingsListItem(
                        title = stringResource(R.string.app_color),
                        subtitle = stringResource(R.string.app_color_desc),
                        leadingIcon = Icons.Rounded.ColorLens,
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = seedColor,
                                        shape = CircleShape
                                    )
                            )
                        },
                        onClick = {
                            showColorPicker = true
                        }
                    )

                    SettingsDivider()

                    SettingsListItem(
                        title = stringResource(R.string.language),
                        subtitle = if (currentLang == "it") {
                            stringResource(R.string.lang_it)
                        } else {
                            stringResource(R.string.lang_en)
                        },
                        leadingIcon = Icons.Rounded.Language,
                        onClick = {
                            showLanguagePicker = true
                        }
                    )
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.planning),
                    icon = Icons.Rounded.AccessTime
                ) {
                    listOf(
                        MealType.BREAKFAST,
                        MealType.MORNING_SNACK,
                        MealType.LUNCH,
                        MealType.AFTERNOON_SNACK,
                        MealType.DINNER,
                        MealType.OTHER
                    ).forEachIndexed { index, type ->
                        if (index > 0) {
                            SettingsDivider()
                        }

                        val timeMinutes =
                            defaultTimesState.find { it.type == type }?.timeMinutes
                                ?: vm.getDefaultFallback(type)

                        SettingsListItem(
                            title = stringResource(type.resId),
                            subtitle = stringResource(R.string.default_time),
                            leadingIcon = Icons.Rounded.AccessTime,
                            trailingContent = {
                                Text(
                                    text = "%02d:%02d".format(
                                        timeMinutes / 60,
                                        timeMinutes % 60
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                mealTypeToEdit = type
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.notifications_alarms),
                    icon = Icons.Rounded.Notifications
                ) {
                    SettingsListItem(
                        title = stringResource(R.string.meal_reminders),
                        subtitle = stringResource(R.string.meal_reminders_desc),
                        leadingIcon = Icons.Rounded.Notifications,
                        trailingContent = {
                            Switch(
                                checked = mealRemindersEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !requestNotificationPermission()) {
                                        return@Switch
                                    }

                                    vm.setMealRemindersEnabled(enabled)
                                }
                            )
                        },
                        onClick = {
                            val next = !mealRemindersEnabled

                            if (next && !requestNotificationPermission()) {
                                return@SettingsListItem
                            }

                            vm.setMealRemindersEnabled(next)
                        }
                    )
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.backup_data),
                    icon = Icons.Rounded.Backup
                ) {
                    SettingsListItem(
                        title = stringResource(R.string.import_diet),
                        subtitle = stringResource(R.string.import_desc),
                        leadingIcon = Icons.Rounded.FileUpload,
                        onClick = {
                            showImportDialog = true
                        }
                    )

                    if (diets.isNotEmpty()) {
                        SettingsDivider()

                        diets.forEachIndexed { index, diet ->
                            if (index > 0) {
                                SettingsDivider()
                            }

                            SettingsListItem(
                                title = stringResource(
                                    R.string.export_name,
                                    diet.name
                                ),
                                leadingIcon = Icons.Rounded.FileDownload,
                                onClick = {
                                    dietToExport = diet.id
                                    exportLauncher.launch(
                                        "${diet.name.lowercase().replace(" ", "_")}.dr"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (isDevMode) {
                item {
                    SettingsSection(
                        title = stringResource(R.string.developer_settings),
                        icon = Icons.Rounded.DeveloperMode
                    ) {
                        SettingsListItem(
                            title = stringResource(R.string.developer_settings),
                            subtitle = stringResource(
                                R.string.developer_settings_description
                            ),
                            leadingIcon = Icons.Rounded.SettingsApplications,
                            onClick = onNavigateToDeveloper
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.app_info),
                    icon = Icons.Rounded.Person
                ) {
                    SettingsListItem(
                        title = stringResource(R.string.version),
                        subtitle = "1.0.0 (Build 20261027)",
                        leadingIcon = Icons.Rounded.Update,
                        onClick = {
                            if (!isDevMode) {
                                devClickCount++

                                if (devClickCount >= 7) {
                                    vm.setDeveloperMode(true)

                                    scope.launch {
                                        snackbarHostState.showSnackbar(greetingMessage)
                                    }

                                    devClickCount = 0
                                } else if (devClickCount > 3) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Ti mancano ${7 - devClickCount} clic"
                                        )
                                    }
                                }
                            }
                        }
                    )

                    SettingsDivider()

                    SettingsListItem(
                        title = stringResource(R.string.developer),
                        subtitle = "Matteo Tatoni",
                        leadingIcon = Icons.Rounded.Person,
                        onClick = {}
                    )
                }
            }
        }
    }

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
                        vm.importDiet(
                            pendingImportJson!!,
                            overwrite = true
                        )

                        showOverwriteDialog = false
                        pendingImportJson = null

                        scope.launch {
                            snackbarHostState.showSnackbar(importSuccessMessage)
                        }
                    }
                ) {
                    Text(stringResource(R.string.overwrite))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOverwriteDialog = false
                        pendingImportJson = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
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
            }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            onDismiss = {
                showColorPicker = false
            },
            onColorSelected = { color ->
                vm.setSeedColor(
                    "0x" + Integer.toHexString(color.toArgb()).uppercase()
                )

                showColorPicker = false
            }
        )
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            onDismiss = {
                showLanguagePicker = false
            },
            onLanguageSelected = { lang ->
                vm.setLanguage(lang)

                val localeManager =
                    context.getSystemService(LocaleManager::class.java)

                localeManager?.applicationLocales =
                    LocaleList.forLanguageTags(lang)

                showLanguagePicker = false
            }
        )
    }

    mealTypeToEdit?.let { type ->
        val currentTime = vm.getDefaultTime(type)

        val timeState = rememberTimePickerState(
            initialHour = currentTime / 60,
            initialMinute = currentTime % 60,
            is24Hour = true
        )

        BasicAlertDialog(
            onDismissRequest = {
                mealTypeToEdit = null
            }
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.set_default_time),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    TimeInput(state = timeState)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                mealTypeToEdit = null
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                vm.saveDefaultTime(
                                    type,
                                    timeState.hour * 60 + timeState.minute
                                )

                                mealTypeToEdit = null
                            }
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsListItem(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = leadingIcon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = trailingContent,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDialog(
    onDismiss: () -> Unit,
    onImportText: (String) -> Unit,
    onPickFile: () -> Unit
) {
    var json by remember { mutableStateOf("") }
    var tabIndex by remember { mutableIntStateOf(0) }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_diet),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                SecondaryTabRow(
                    selectedTabIndex = tabIndex
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = {
                            tabIndex = 0
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.import_text),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Tab(
                        selected = tabIndex == 1,
                        onClick = {
                            tabIndex = 1
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.import_file),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (tabIndex == 0) {
                    OutlinedTextField(
                        value = json,
                        onValueChange = {
                            json = it
                        },
                        label = {
                            Text(stringResource(R.string.json_content))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onPickFile
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileOpen,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(stringResource(R.string.import_file))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    if (tabIndex == 0) {
                        Button(
                            onClick = {
                                onImportText(json)
                            },
                            enabled = json.isNotBlank()
                        ) {
                            Text(stringResource(R.string.import_label))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color(0xFF6750A4),
        Color(0xFF9C27B0),
        Color(0xFFE91E63),
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFF4CAF50),
        Color(0xFF00BCD4),
        Color(0xFF2196F3)
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.choose_seed_color),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = color,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onColorSelected(color)
                                }
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    )
                )

                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.lang_it))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        onLanguageSelected("it")
                    }
                )

                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.lang_en))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        onLanguageSelected("en")
                    }
                )

                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}