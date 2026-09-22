package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.ui.components.ScheduledAlarmRow
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DeveloperAlarmsSection(
    alarms: List<ScheduledAlarm>,
    onCancelAlarm: (ScheduledAlarm) -> Unit,
    onRunChecker: () -> Unit,
    onClearAlarms: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(
            title = stringResource(R.string.scheduled_alarms),
            icon = Icons.Rounded.Alarm,
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (alarms.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_scheduled_alarms),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        alarms.forEach { alarm ->
                            ScheduledAlarmRow(
                                alarm = alarm,
                                onCancel = { onCancelAlarm(alarm) },
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onRunChecker,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.run_checker))
                    }

                    OutlinedButton(
                        onClick = onClearAlarms,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.clear_alarms))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeveloperAlarmsSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DeveloperAlarmsSection(
                alarms = listOf(
                    ScheduledAlarm(1, "MEAL", System.currentTimeMillis() + 3600000, "Lunch"),
                    ScheduledAlarm(2, "HYDRATION", System.currentTimeMillis() + 7200000, "Water"),
                ),
                onCancelAlarm = {},
                onRunChecker = {},
                onClearAlarms = {},
            )
        }
    }
}