package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek

@Composable
fun HydrationScreen(vm: DietViewModel) {
    val hydrationEnabled by vm.hydrationEnabled.collectAsState()
    val hydrationInterval by vm.hydrationInterval.collectAsState()
    val hydrationRanges by vm.hydrationRanges.collectAsState()
    val activeDays by vm.hydrationDays.collectAsState()

    var showAddRangeDialog by rememberSaveable { mutableStateOf(false) }

    HydrationContent(
        hydrationEnabled = hydrationEnabled,
        hydrationInterval = hydrationInterval,
        hydrationRanges = hydrationRanges,
        activeDays = activeDays,
        onEnabledChange = vm::setHydrationEnabled,
        onIntervalChange = vm::setHydrationInterval,
        onDaysChange = vm::setHydrationDays,
        onDeleteRange = { range ->
            vm.setHydrationRanges(hydrationRanges - range)
        },
        onAddRangeClick = { showAddRangeDialog = true }
    )

    if (showAddRangeDialog) {
        AddHydrationRangeDialog(
            onDismiss = { showAddRangeDialog = false },
            onAdd = { start, end ->
                vm.setHydrationRanges(
                    hydrationRanges + HydrationRange(
                        startMinutes = start,
                        endMinutes = end,
                    ),
                )
                showAddRangeDialog = false
            },
        )
    }
}

@Composable
fun HydrationContent(
    hydrationEnabled: Boolean,
    hydrationInterval: Int,
    hydrationRanges: List<HydrationRange>,
    activeDays: Set<DayOfWeek>,
    onEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onDeleteRange: (HydrationRange) -> Unit,
    onAddRangeClick: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            if (hydrationEnabled) {
                FloatingActionButton(
                    onClick = onAddRangeClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add),
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = if (hydrationEnabled) 104.dp else 32.dp,
            ),
        ) {
            item {
                HydrationHeader()
            }

            item {
                HydrationSettingsSection(
                    enabled = hydrationEnabled,
                    onEnabledChange = onEnabledChange,
                )
            }

            if (hydrationEnabled) {
                item {
                    HydrationIntervalSection(
                        interval = hydrationInterval,
                        onIntervalChange = onIntervalChange,
                    )
                }

                item {
                    HydrationDaysSection(
                        activeDays = activeDays,
                        onDaysChange = onDaysChange,
                    )
                }

                item {
                    HydrationWindowsSection(
                        ranges = hydrationRanges,
                        onDelete = onDeleteRange,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HydrationContentPreview() {
    DietTheme {
        HydrationContent(
            hydrationEnabled = true,
            hydrationInterval = 60,
            hydrationRanges = listOf(
                HydrationRange(540, 720),
                HydrationRange(840, 1020),
            ),
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            onEnabledChange = {},
            onIntervalChange = {},
            onDaysChange = {},
            onDeleteRange = {},
            onAddRangeClick = {}
        )
    }
}