package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.permission.rememberPermissionRequester
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
        onScheduledTrigger = {
            vm.scheduleTestAlarm(10)
        },
        onResetDatabase = {},
        onDisableDeveloperMode = {
            vm.setDeveloperMode(false)
            onBack()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onScheduledTrigger: () -> Unit,
    onResetDatabase: () -> Unit,
    onDisableDeveloperMode: () -> Unit,
) {
    var selectedLevel by remember { mutableStateOf(LogLevel.TRACE) }

    val filteredLogs = remember(logs, selectedLevel) {
        logs.filter { it.level.priority >= selectedLevel.priority }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.developer_settings))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                DeveloperPermissionsSection(
                    onCheckPermissions = onCheckPermissions,
                )
            }

            item {
                DeveloperLogSection(
                    logs = filteredLogs,
                    selectedLevel = selectedLevel,
                    onLevelSelected = { selectedLevel = it },
                    onClearLogs = onClearLogs,
                )
            }

            item {
                DeveloperAlarmsSection(
                    alarms = scheduledAlarms,
                    onCancelAlarm = onCancelAlarm,
                    onRunChecker = onRunChecker,
                    onClearAlarms = onClearAlarms,
                )
            }

            item {
                DeveloperNotificationSection(
                    onScheduledTrigger = onScheduledTrigger,
                )
            }

            item {
                DeveloperDangerZone(
                    onResetDatabase = onResetDatabase,
                    onDisableDeveloperMode = onDisableDeveloperMode,
                )
            }
        }
    }
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
            onScheduledTrigger = {},
            onResetDatabase = {},
            onDisableDeveloperMode = {},
        )
    }
}
