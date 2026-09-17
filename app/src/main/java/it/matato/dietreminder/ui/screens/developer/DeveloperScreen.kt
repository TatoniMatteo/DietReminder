package it.matato.dietreminder.ui.screens.developer

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import it.matato.dietreminder.R
import it.matato.dietreminder.data.ScheduledAlarm
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import it.matato.dietreminder.util.AlarmScheduler
import it.matato.dietreminder.util.AlarmSyncHelper
import it.matato.dietreminder.util.AlarmTracker
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.LogEntry
import it.matato.dietreminder.util.LogLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeveloperScreen(
    vm: DietViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val logs by vm.appLogs.collectAsState()

    var selectedLevel by remember { mutableStateOf(LogLevel.TRACE) }
    var showResetDialog by remember { mutableStateOf(false) }
    var scheduledAlarms by remember { mutableStateOf<List<ScheduledAlarm>>(emptyList()) }

    val scope = rememberCoroutineScope()

    val filteredLogs = remember(logs, selectedLevel) {
        logs.filter { it.level.priority >= selectedLevel.priority }
    }

    val refreshAlarms = {
        scope.launch {
            scheduledAlarms = AlarmTracker.getAlarms(context)
        }
    }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        AppLog.d("Returned from exact alarm permission settings")
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        val alarmManager =
            context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }

            exactAlarmLauncher.launch(intent)
        }
    }

    fun checkPermissions() {
        val notificationGranted = context.checkSelfPermission(
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!notificationGranted) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
            return
        }

        val alarmManager =
            context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }

            exactAlarmLauncher.launch(intent)
        }
    }

    LaunchedEffect(Unit) {
        refreshAlarms()
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
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                DeveloperSectionTitle(
                    title = stringResource(R.string.developer_settings)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.check_app_permissions),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = stringResource(
                                        R.string.check_app_permissions_description
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                checkPermissions()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(stringResource(R.string.check_app_permissions))
                        }
                    }
                }
            }

            item {
                DeveloperSectionTitle(
                    title = stringResource(R.string.log_levels)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.log_levels),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LogLevel.entries
                                .sortedBy { it.priority }
                                .forEach { level ->
                                    LogLevelChip(
                                        label = level.name,
                                        selected = selectedLevel == level,
                                        onClick = {
                                            selectedLevel = level
                                        }
                                    )
                                }
                        }
                    }
                }
            }

            item {
                DeveloperSectionTitle(
                    title = stringResource(R.string.log_console)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(10.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredLogs) { entry ->
                                    LogLine(entry)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    AppLog.clear()
                                }
                            ) {
                                Text(stringResource(R.string.clear_logs))
                            }
                        }
                    }
                }
            }

            item {
                DeveloperSectionTitle(
                    title = stringResource(R.string.scheduled_alarms)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (scheduledAlarms.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_scheduled_alarms),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            scheduledAlarms.forEach { alarm ->
                                ScheduledAlarmRow(
                                    alarm = alarm,
                                    onCancel = {
                                        AlarmScheduler.cancelAlarm(
                                            context,
                                            alarm.id
                                        )
                                        refreshAlarms()
                                    }
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    AlarmSyncHelper.syncAlarms(context)
                                    refreshAlarms()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.run_checker))
                            }

                            OutlinedButton(
                                onClick = {
                                    AlarmScheduler.cancelAllAlarms(context)
                                    refreshAlarms()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.clear_alarms))
                            }
                        }
                    }
                }
            }

            item {
                DeveloperSectionTitle(
                    title = stringResource(R.string.notification_tests)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            vm.triggerTestAlarm()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(stringResource(R.string.immediate_trigger))
                    }

                    OutlinedButton(
                        onClick = {
                            vm.scheduleTestAlarm(10)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(stringResource(R.string.test_scheduler))
                    }
                }
            }

            item {
                DangerZone(
                    onResetDatabase = {
                        showResetDialog = true
                    }
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
            },
            title = {
                Text(
                    text = stringResource(R.string.reset_database),
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(stringResource(R.string.reset_database_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.resetDatabase()
                        showResetDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DangerZone(
    onResetDatabase: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.danger_zone),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reset_database),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = stringResource(R.string.reset_database_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onResetDatabase,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(stringResource(R.string.reset_database))
                }
            }
        }
    }
}

@Composable
private fun DeveloperSectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ScheduledAlarmRow(
    alarm: ScheduledAlarm,
    onCancel: () -> Unit
) {
    val locale = LocalLocale.current.platformLocale

    val time = Instant.ofEpochMilli(alarm.timeMillis)
        .atZone(ZoneId.systemDefault())
        .format(
            DateTimeFormatter.ofPattern(
                "EEE HH:mm",
                locale
            )
        )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = alarm.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(
                    R.string.alarm_time_and_id,
                    time,
                    alarm.id
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onCancel
        ) {
            Icon(
                imageVector = Icons.Rounded.Cancel,
                contentDescription = stringResource(R.string.cancel_alarm),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LogLevelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label)
        }
    )
}

@Composable
private fun LogLine(
    entry: LogEntry
) {
    val color = when (entry.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> Color(0xFFFFA000)
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.TRACE -> MaterialTheme.colorScheme.outline
    }

    Text(
        text = "[${entry.timestamp}] ${entry.level.name.take(1)}: ${entry.message}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = color
    )
}