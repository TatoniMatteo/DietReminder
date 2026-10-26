package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.DeveloperLogLine
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.util.LogEntry
import it.matato.dietreminder.util.LogLevel

@Composable
fun DeveloperLogSection(
    logs: List<LogEntry>,
    selectedLevel: LogLevel,
    onLevelSelected: (LogLevel) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var levelMenu by remember { mutableStateOf(false) }
    val visibleLogs = remember(logs, selectedLevel, query) {
        logs.filter { it.level.priority >= selectedLevel.priority &&
            (query.isBlank() || it.message.contains(query.trim(), ignoreCase = true) || it.timestamp.contains(query.trim())) }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(query, selectedLevel) { listState.scrollToItem(0) }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("developerLogsList"),
        state = listState,
        contentPadding = PaddingValues(horizontal = AppSpace.screen, vertical = AppSpace.group),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item("filters") {
            Column(Modifier.padding(bottom = AppSpace.item), verticalArrangement = Arrangement.spacedBy(AppSpace.inline)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag("developerLogSearch"),
                    label = { Text(stringResource(R.string.developer_log_search)) },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.inline)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { levelMenu = true }, modifier = Modifier.testTag("developerLogLevel")) {
                            Text(if (selectedLevel == LogLevel.TRACE) stringResource(R.string.developer_log_all_levels)
                                else stringResource(R.string.developer_log_min_level, selectedLevel.name), modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = levelMenu, onDismissRequest = { levelMenu = false }) {
                            LogLevel.entries.sortedBy { it.priority }.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(if (level == LogLevel.TRACE) stringResource(R.string.developer_log_all_levels)
                                        else stringResource(R.string.developer_log_min_level, level.name)) },
                                    trailingIcon = { if (level == selectedLevel) Icon(Icons.Rounded.Check, null) },
                                    modifier = Modifier.testTag("developerLogLevel_${level.name}"),
                                    onClick = { onLevelSelected(level); levelMenu = false },
                                )
                            }
                        }
                    }
                    IconButton(onClick = onClearLogs, enabled = logs.isNotEmpty(), modifier = Modifier.testTag("developerClearLogs")) {
                        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.clear_logs))
                    }
                }
            }
        }
        item("count") {
            Column(Modifier.padding(vertical = AppSpace.small), verticalArrangement = Arrangement.spacedBy(AppSpace.small)) {
                Text(stringResource(R.string.developer_log_count, visibleLogs.size, logs.size), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.developer_log_order), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (visibleLogs.isEmpty()) item("empty") {
            DeveloperEmptyState(
                title = stringResource(if (logs.isEmpty()) R.string.developer_logs_empty else R.string.developer_logs_no_matches),
                description = stringResource(if (logs.isEmpty()) R.string.developer_logs_empty_description else R.string.developer_logs_no_matches_description),
                icon = Icons.Rounded.Description,
            )
        }
        itemsIndexed(visibleLogs) { index, entry ->
            DeveloperLogLine(entry, Modifier.testTag("developerLogEntry_$index"))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}
