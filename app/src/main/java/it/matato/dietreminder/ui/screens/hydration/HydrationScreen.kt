package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun HydrationScreen(
    vm: DietViewModel, padding: PaddingValues
) {
    val hydrationEnabled by vm.hydrationEnabled.collectAsState()
    val hydrationInterval by vm.hydrationInterval.collectAsState()
    val hydrationRanges by vm.hydrationRanges.collectAsState()
    val activeDays by vm.hydrationDays.collectAsState()

    var showAddRangeDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(padding), floatingActionButton = {
            if (hydrationEnabled) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        showAddRangeDialog = true
                    }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add, contentDescription = stringResource(R.string.add)
                    )
                }
            }
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 24.dp, bottom = if (hydrationEnabled) 104.dp else 32.dp
            )
        ) {
            item {
                HydrationHeader()
            }

            item {
                HydrationSettingsSection(
                    enabled = hydrationEnabled, onEnabledChange = vm::setHydrationEnabled
                )
            }

            if (hydrationEnabled) {
                item {
                    HydrationIntervalSection(
                        interval = hydrationInterval, onIntervalChange = vm::setHydrationInterval
                    )
                }

                item {
                    HydrationDaysSection(
                        activeDays = activeDays, onDaysChange = vm::setHydrationDays
                    )
                }

                item {
                    HydrationWindowsSectionHeader(
                        count = hydrationRanges.size
                    )
                }

                if (hydrationRanges.isEmpty()) {
                    item {
                        EmptyHydrationWindows()
                    }
                } else {
                    item {
                        HydrationWindowsList(
                            ranges = hydrationRanges, onDelete = { range ->
                                vm.setHydrationRanges(
                                    hydrationRanges - range
                                )
                            })
                    }
                }
            }
        }
    }

    if (showAddRangeDialog) {
        AddHydrationRangeDialog(onDismiss = {
            showAddRangeDialog = false
        }, onAdd = { start, end ->
            vm.setHydrationRanges(
                hydrationRanges + HydrationRange(start, end)
            )
            showAddRangeDialog = false
        })
    }
}

@Composable
private fun HydrationHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.hydration_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.hydration_reminders_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HydrationSettingsSection(
    enabled: Boolean, onEnabledChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            title = stringResource(R.string.hydration_reminders), icon = Icons.Rounded.WaterDrop
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
        ) {
            ListItem(headlineContent = {
                Text(
                    text = stringResource(R.string.hydration_reminders), fontWeight = FontWeight.SemiBold
                )
            }, supportingContent = {
                Text(
                    text = stringResource(R.string.hydration_reminders_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }, leadingContent = {
                IconContainer(
                    enabled = enabled, icon = Icons.Rounded.WaterDrop
                )
            }, trailingContent = {
                Switch(
                    checked = enabled, onCheckedChange = onEnabledChange
                )
            })
        }
    }
}

@Composable
private fun HydrationIntervalSection(
    interval: Int, onIntervalChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            title = stringResource(R.string.hydration_interval), icon = Icons.Rounded.Schedule
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 20.dp, vertical = 18.dp
                ), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.hydration_interval),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            )
                            .padding(
                                horizontal = 12.dp, vertical = 6.dp
                            )
                    ) {
                        Text(
                            text = stringResource(
                                R.string.window_minutes_value, interval
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Slider(
                    value = interval.toFloat(), onValueChange = {
                        onIntervalChange(it.toInt())
                    }, valueRange = 15f .. 180f, steps = 10
                )

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "15 min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "180 min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HydrationDaysSection(
    activeDays: Set<DayOfWeek>, onDaysChange: (Set<DayOfWeek>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            title = stringResource(R.string.hydration_days), icon = Icons.Rounded.Schedule
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in activeDays

                    HydrationDayButton(
                        day = day, selected = selected, onClick = {
                            onDaysChange(
                                if (selected) {
                                    activeDays - day
                                } else {
                                    activeDays + day
                                }
                            )
                        })
                }
            }
        }
    }
}

@Composable
private fun HydrationDayButton(
    day: DayOfWeek, selected: Boolean, onClick: () -> Unit
) {
    val today = day == LocalDate.now().dayOfWeek

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    when {
                        selected -> MaterialTheme.colorScheme.primary
                        today -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .clickable(onClick = onClick), contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.getDisplayName(
                    TextStyle.NARROW, LocalLocale.current.platformLocale
                ).uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    today -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (today) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    }
                )
        )
    }
}

@Composable
private fun HydrationWindowsSectionHeader(
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            title = stringResource(R.string.hydration_windows), icon = Icons.Rounded.Schedule, modifier = Modifier.weight(1f)
        )

        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(
                        horizontal = 10.dp, vertical = 5.dp
                    )
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HydrationWindowsList(
    ranges: List<HydrationRange>, onDelete: (HydrationRange) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
    ) {
        Column {
            ranges.forEachIndexed { index, range ->
                HydrationRangeItem(
                    range = range, onDelete = {
                        onDelete(range)
                    })

                if (index < ranges.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HydrationRangeItem(
    range: HydrationRange, onDelete: () -> Unit
) {
    var menuExpanded by rememberSaveable(
        range.startMinutes, range.endMinutes
    ) {
        mutableStateOf(false)
    }

    ListItem(headlineContent = {
        Text(
            text = stringResource(
                R.string.from_to,
                range.startMinutes / 60,
                range.startMinutes % 60,
                range.endMinutes / 60,
                range.endMinutes % 60
            ), fontWeight = FontWeight.SemiBold
        )
    }, supportingContent = {
        Text(
            text = stringResource(R.string.hydration_windows), color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }, leadingContent = {
        IconContainer(
            enabled = true, icon = Icons.Rounded.Schedule
        )
    }, trailingContent = {
        Box {
            IconButton(
                onClick = {
                    menuExpanded = true
                }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.delete)
                )
            }

            DropdownMenu(
                expanded = menuExpanded, onDismissRequest = {
                    menuExpanded = false
                }) {
                DropdownMenuItem(text = {
                    Text(
                        text = stringResource(R.string.delete), color = MaterialTheme.colorScheme.error
                    )
                }, leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }, onClick = {
                    menuExpanded = false
                    onDelete()
                })
            }
        }
    })
}

@Composable
private fun EmptyHydrationWindows() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconContainer(
                enabled = false, icon = Icons.Rounded.Schedule, size = 56.dp, iconSize = 28.dp
            )

            Text(
                text = stringResource(R.string.hydration_windows),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.new_window),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IconContainer(
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    iconSize: androidx.compose.ui.unit.Dp = 21.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ), contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon, contentDescription = null, tint = if (enabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }, modifier = Modifier.size(iconSize)
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AddHydrationRangeDialog(
    onDismiss: () -> Unit, onAdd: (Int, Int) -> Unit
) {
    val startState = rememberTimePickerState(
        initialHour = 9, initialMinute = 0, is24Hour = true
    )

    val endState = rememberTimePickerState(
        initialHour = 18, initialMinute = 0, is24Hour = true
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconContainer(
                        enabled = true, icon = Icons.Rounded.Schedule, size = 50.dp, iconSize = 26.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = stringResource(R.string.new_window),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.start),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    TimeInput(state = startState)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.end),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    TimeInput(state = endState)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            onAdd(
                                startState.hour * 60 + startState.minute, endState.hour * 60 + endState.minute
                            )
                        }) {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
}
