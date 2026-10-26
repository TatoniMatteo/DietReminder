package it.matato.dietreminder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.data.model.resId
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import it.matato.dietreminder.ui.theme.AppSpace
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduledAlarmRow(alarm: ScheduledAlarm, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalLocale.current.platformLocale
    val time = Instant.ofEpochMilli(alarm.timeMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val type = MealType.entries.find { it.name == alarm.type }?.let { stringResource(it.resId) }
        ?: if (alarm.type == "DAILY_SYNC") stringResource(R.string.daily_alarm_sync) else alarm.type
    AppCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppSpace.card), verticalArrangement = Arrangement.spacedBy(AppSpace.item)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.item)) {
                Text(time, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f, fill = false)) {
                    Text(type, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(alarm.label, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.developer_alarm_id, alarm.id), modifier = Modifier.padding(vertical = 14.dp),
                    style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onCancel, modifier = Modifier.testTag("developerCancelAlarm_${alarm.id}"), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Rounded.Cancel, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpace.inline))
                    Text(stringResource(R.string.cancel_alarm))
                }
            }
        }
    }
}
