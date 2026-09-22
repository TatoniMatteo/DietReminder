package it.matato.dietreminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.ui.theme.DietTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScheduledAlarmRow(
    alarm: ScheduledAlarm,
    onCancel: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale

    val time = Instant.ofEpochMilli(alarm.timeMillis).atZone(ZoneId.systemDefault()).format(
        DateTimeFormatter.ofPattern("EEE HH:mm", locale),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = alarm.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = stringResource(
                    R.string.alarm_time_and_id,
                    time,
                    alarm.id,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Rounded.Cancel,
                contentDescription = stringResource(R.string.cancel_alarm),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduledAlarmRowPreview() {
    DietTheme {
        ScheduledAlarmRow(
            alarm = ScheduledAlarm(
                id = 1,
                type = "MEAL",
                timeMillis = System.currentTimeMillis(),
                label = "Lunch Time",
            ),
            onCancel = {},
        )
    }
}
