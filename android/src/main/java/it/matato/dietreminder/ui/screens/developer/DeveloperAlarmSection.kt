package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.ui.components.ScheduledAlarmRow
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import it.matato.dietreminder.ui.theme.AppSpace
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperAlarmsSection(
    alarms: List<ScheduledAlarm>,
    onCancelAlarm: (ScheduledAlarm) -> Unit,
    onRunChecker: () -> Unit,
    onClearAlarms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val zone = ZoneId.systemDefault()
    val groups = remember(alarms, zone) {
        alarms.sortedWith(compareBy<ScheduledAlarm> { it.timeMillis }.thenBy { it.id })
            .groupBy { Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate() }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("developerAlarmsList"),
        contentPadding = PaddingValues(horizontal = AppSpace.screen, vertical = AppSpace.group),
        verticalArrangement = Arrangement.spacedBy(AppSpace.item),
    ) {
        item("overview") {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(AppSpace.card), verticalArrangement = Arrangement.spacedBy(AppSpace.item)) {
                    Text(stringResource(R.string.developer_alarm_count, alarms.size), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.developer_alarm_summary), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.inline)) {
                        FilledTonalButton(onClick = onRunChecker, modifier = Modifier.testTag("developerSyncAlarms")) {
                            Icon(Icons.Rounded.Update, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(AppSpace.inline))
                            Text(stringResource(R.string.run_checker))
                        }
                        TextButton(onClick = onClearAlarms, enabled = alarms.isNotEmpty(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("developerClearAlarms")) {
                            Text(stringResource(R.string.clear_alarms))
                        }
                    }
                }
            }
        }
        if (alarms.isEmpty()) item("empty") {
            DeveloperEmptyState(stringResource(R.string.no_scheduled_alarms),
                stringResource(R.string.developer_alarms_empty_description), Icons.Rounded.Alarm)
        }
        groups.forEach { (date, entries) ->
            item("date-$date") {
                Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                    modifier = Modifier.padding(top = AppSpace.item, bottom = AppSpace.small),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(entries, key = { it.id }) { alarm ->
                ScheduledAlarmRow(alarm, onCancel = { onCancelAlarm(alarm) }, modifier = Modifier.testTag("developerAlarm_${alarm.id}"))
            }
        }
    }
}
