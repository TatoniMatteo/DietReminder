package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.DeveloperLogLine
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.util.LogEntry
import it.matato.dietreminder.util.LogLevel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperLogSection(
    logs: List<LogEntry>,
    selectedLevel: LogLevel,
    onLevelSelected: (LogLevel) -> Unit,
    onClearLogs: () -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle(
        title = stringResource(R.string.log_console),
        icon = androidx.compose.material.icons.Icons.Rounded.Description
    )

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.log_levels),
                style = MaterialTheme.typography.titleSmall
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogLevel.entries.sortedBy { it.priority }.forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { onLevelSelected(level) },
                        label = { Text(level.name) })
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                    .padding(10.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { entry ->
                        DeveloperLogLine(entry)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearLogs) {
                    Text(stringResource(R.string.clear_logs))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeveloperLogSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DeveloperLogSection(
                logs = listOf(
                    LogEntry("12:00:00", LogLevel.INFO, "Application started"),
                    LogEntry("12:00:01", LogLevel.DEBUG, "Loading settings..."),
                    LogEntry("12:00:02", LogLevel.ERROR, "Failed to load settings"),
                ),
                selectedLevel = LogLevel.TRACE,
                onLevelSelected = {},
                onClearLogs = {},
            )
        }
    }
}
