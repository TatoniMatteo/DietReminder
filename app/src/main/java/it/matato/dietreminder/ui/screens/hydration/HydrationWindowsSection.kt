package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun HydrationWindowsSection(
    ranges: List<HydrationRange>,
    onDelete: (HydrationRange) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HydrationWindowsSectionHeader(count = ranges.size)

        if (ranges.isEmpty()) {
            EmptyHydrationWindows()
        } else {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Column {
                    ranges.forEachIndexed { index, range ->
                        HydrationRangeItem(
                            range = range,
                            onDelete = { onDelete(range) },
                        )

                        if (index < ranges.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HydrationWindowsSectionHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            title = stringResource(R.string.hydration_windows),
            icon = Icons.Rounded.Schedule,
            modifier = Modifier.weight(1f),
        )

        if (count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(28.dp),
            )
        }
    }
}

@Composable
private fun EmptyHydrationWindows() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconContainer(
                icon = Icons.Rounded.Schedule,
                size = 56.dp,
                iconSize = 28.dp,
            )

            Text(
                text = stringResource(R.string.hydration_windows),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.new_window),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HydrationWindowsSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HydrationWindowsSection(
                ranges = listOf(
                    HydrationRange(540, 720),
                    HydrationRange(840, 1020),
                ),
                onDelete = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyHydrationWindowsPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HydrationWindowsSection(
                ranges = emptyList(),
                onDelete = {},
            )
        }
    }
}