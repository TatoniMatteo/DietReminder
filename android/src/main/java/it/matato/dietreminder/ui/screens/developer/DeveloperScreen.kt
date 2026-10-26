package it.matato.dietreminder.ui.screens.developer

import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.components.AppTopBar
import it.matato.dietreminder.ui.components.AppScaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import it.matato.dietreminder.ui.screens.settings.SettingsListItem
import it.matato.dietreminder.ui.screens.settings.SettingsDivider
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Box
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.util.permission.rememberPermissionRequester
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.LogEntry
import it.matato.dietreminder.util.LogLevel
import it.matato.dietreminder.util.alarm.AlarmScheduler
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.util.alarm.AlarmTracker
import it.matato.dietreminder.viewmodel.DietViewModel

@Composable
fun DeveloperScreen(
	vm: DietViewModel,
	onBack: () -> Unit,
) {
	val context = LocalContext.current
	val logs by vm.appLogs.collectAsState()
	val scheduledAlarms by AlarmTracker.observeAlarms(context).collectAsState(initial = emptyList())

	val requestPermissions = rememberPermissionRequester(context)

	DeveloperContent(
		logs = logs,
		scheduledAlarms = scheduledAlarms,
		onBack = onBack,
		onCheckPermissions = requestPermissions,
		onClearLogs = AppLog::clear,
		onCancelAlarm = { alarm ->
			AlarmScheduler.cancelAlarm(context, alarm.id)
		},
		onRunChecker = {
			AlarmSyncHelper.syncAlarms(context)
		},
		onClearAlarms = {
			AlarmScheduler.cancelAllAlarms(context)
		},

		onResetDatabase = vm::resetDatabase,
		onDisableDeveloperMode = {
			vm.setDeveloperMode(false)
			onBack()
		},
	)
}

@Composable
fun DeveloperContent(
	logs: List<LogEntry>,
	scheduledAlarms: List<ScheduledAlarm>,
	onBack: () -> Unit,
	onCheckPermissions: () -> Unit,
	onClearLogs: () -> Unit,
	onCancelAlarm: (ScheduledAlarm) -> Unit,
	onRunChecker: () -> Unit,
	onClearAlarms: () -> Unit,
	onResetDatabase: () -> Unit,
	onDisableDeveloperMode: () -> Unit,
) {
	var selectedLevel by rememberSaveable { mutableStateOf(LogLevel.TRACE) }
    var pageKey by rememberSaveable { mutableStateOf<String?>(null) }
    val page = DeveloperPage.entries.find { it.name == pageKey }
    val goBack: () -> Unit = { if (page != null) pageKey = null else onBack() }
    androidx.activity.compose.BackHandler(enabled = page != null, onBack = goBack)

	AppScaffold(
		topBar = {
			AppTopBar(
				title = {
					Text(stringResource(page?.title ?: R.string.developer_settings))
				},
				navigationIcon = {
					IconButton(onClick = goBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
							contentDescription = stringResource(R.string.back),
						)
					}
				},
			)
		},
	) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (page) {
            DeveloperPage.LOGS -> DeveloperLogSection(logs, selectedLevel, { selectedLevel = it }, onClearLogs, contentModifier)
            DeveloperPage.ALARMS -> DeveloperAlarmsSection(scheduledAlarms, onCancelAlarm, onRunChecker, onClearAlarms, contentModifier)
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(horizontal = AppSpace.screen, vertical = AppSpace.group),
                verticalArrangement = Arrangement.spacedBy(AppSpace.section),
            ) {
                if (page == null) {
                    item("tools") {
                        it.matato.dietreminder.ui.components.AppCard {
                            DeveloperPage.entries.forEachIndexed { index, entry ->
                                if (index > 0) SettingsDivider()
                                Box(Modifier.testTag("developerPage_${entry.name}")) {
                                    SettingsListItem(stringResource(entry.title), stringResource(entry.description), entry.icon,
                                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                                        onClick = { pageKey = entry.name })
                                }
                            }
                        }
                    }
                } else item(page.name) {
                    when (page) {
                        DeveloperPage.PERMISSIONS -> DeveloperPermissionsSection(onCheckPermissions)
                        DeveloperPage.MANAGEMENT -> DeveloperDangerZone(onResetDatabase, onDisableDeveloperMode)
                        DeveloperPage.ALARMS, DeveloperPage.LOGS -> Unit // Each has its own full-height list above.
                    }
                }
            }
        }
    }
}

private enum class DeveloperPage(val title: Int, val description: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PERMISSIONS(R.string.developer_permissions, R.string.developer_permissions_summary, Icons.Rounded.Security),
    ALARMS(R.string.scheduled_alarms, R.string.developer_alarms_summary, Icons.Rounded.Alarm),
    LOGS(R.string.log_console, R.string.developer_logs_summary, Icons.Rounded.Description),
    MANAGEMENT(R.string.danger_zone, R.string.developer_danger_summary, Icons.Rounded.Settings),
}

@Preview(showBackground = true)
@Composable
private fun DeveloperContentPreview() {
	DietTheme {
		DeveloperContent(
			logs = listOf(
				LogEntry("12:00:00", LogLevel.INFO, "Application started"),
				LogEntry("12:00:01", LogLevel.DEBUG, "Loading settings..."),
			),
			scheduledAlarms = listOf(
				ScheduledAlarm(
					1,
					"MEAL",
					System.currentTimeMillis() + 3600000,
					"Lunch",
				),
			),
			onBack = {},
			onCheckPermissions = {},
			onClearLogs = {},
			onCancelAlarm = {},
			onRunChecker = {},
			onClearAlarms = {},
			onResetDatabase = {},
			onDisableDeveloperMode = {},
		)
	}
}
