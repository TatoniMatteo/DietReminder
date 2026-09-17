package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HydrationScreen(
    vm: DietViewModel,
    padding: PaddingValues
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val hydrationEnabled by vm.hydrationEnabled.collectAsState()
    val hydrationInterval by vm.hydrationInterval.collectAsState()
    val hydrationRanges by vm.hydrationRanges.collectAsState()
    val activeDays by vm.hydrationDays.collectAsState()

    var showAddRangeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(padding)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.hydration_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.hydration_reminders), fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(stringResource(R.string.hydration_reminders_desc)) },
                        leadingContent = { Icon(Icons.Rounded.WaterDrop, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            Switch(checked = hydrationEnabled, onCheckedChange = { vm.setHydrationEnabled(it) })
                        }
                    )
                }
            }

            if (hydrationEnabled) {
                item {
                    Text(stringResource(R.string.hydration_interval), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Slider(
                                value = hydrationInterval.toFloat(),
                                onValueChange = { vm.setHydrationInterval(it.toInt()) },
                                valueRange = 15f..180f,
                                steps = 10
                            )
                            Text(
                                text = "$hydrationInterval min",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                item {
                    Text(stringResource(R.string.hydration_days), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val isSelected = activeDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newDays = if (isSelected) activeDays - day else activeDays + day
                                    vm.setHydrationDays(newDays)
                                },
                                label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.hydration_windows), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAddRangeDialog = true }) {
                            Icon(Icons.Rounded.Add, null)
                        }
                    }
                }

                items(hydrationRanges) { range ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.from_to, range.startMinutes / 60, range.startMinutes % 60, range.endMinutes / 60, range.endMinutes % 60),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(onClick = {
                                vm.setHydrationRanges(hydrationRanges - range)
                            }) {
                                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRangeDialog) {
        AddHydrationRangeDialog(
            onDismiss = { showAddRangeDialog = false },
            onAdd = { start, end ->
                vm.setHydrationRanges(hydrationRanges + HydrationRange(start, end))
                showAddRangeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHydrationRangeDialog(
    onDismiss: () -> Unit,
    onAdd: (Int, Int) -> Unit
) {
    val startState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
    val endState = rememberTimePickerState(initialHour = 18, initialMinute = 0, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onAdd(startState.hour * 60 + startState.minute, endState.hour * 60 + endState.minute)
            }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.new_window)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.start), style = MaterialTheme.typography.labelLarge)
                TimeInput(state = startState)
                Text(stringResource(R.string.end), style = MaterialTheme.typography.labelLarge)
                TimeInput(state = endState)
            }
        }
    )
}
