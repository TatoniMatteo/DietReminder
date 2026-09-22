package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun HydrationDaysSection(
    activeDays: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(
            title = stringResource(R.string.hydration_days),
            icon = Icons.Rounded.Schedule,
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DayOfWeek.entries.forEach { day ->
                    HydrationDayButton(
                        day = day,
                        selected = day in activeDays,
                        onClick = {
                            onDaysChange(
                                if (day in activeDays) {
                                    activeDays - day
                                } else {
                                    activeDays + day
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HydrationDayButton(
    day: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val today = day == LocalDate.now().dayOfWeek

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    },
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.getDisplayName(
                    TextStyle.NARROW,
                    LocalLocale.current.platformLocale,
                ).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    today -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
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
                        Color.Transparent
                    },
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HydrationDaysSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HydrationDaysSection(
                activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                onDaysChange = {},
            )
        }
    }
}