package it.matato.dietreminder.ui.screens.mealdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun MealInformationSection(
    timeMinutes: Int,
    onTimeClick: () -> Unit,
    type: MealType,
    onTypeChange: (MealType) -> Unit,
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.time_label),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.meal_time),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    IconContainer(icon = Icons.Rounded.Schedule)
                },
                trailingContent = {
                    OutlinedButton(onClick = onTimeClick) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(formatTime(timeMinutes))
                    }
                },
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
            )

            Box {
                ListItem(
                    modifier = Modifier.clickable {
                        typeMenuExpanded = true
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.meal_type),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(type.resId),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        IconContainer(icon = Icons.Rounded.Restaurant)
                    },
                )

                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = {
                        typeMenuExpanded = false
                    },
                ) {
                    MealType.entries.forEach { mealType ->
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(mealType.resId))
                            },
                            onClick = {
                                onTypeChange(mealType)
                                typeMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMinutes: Int): String {
    return "%02d:%02d".format(
        timeMinutes / 60,
        timeMinutes % 60,
    )
}

@Preview(showBackground = true)
@Composable
private fun MealInformationSectionPreview() {
    DietTheme {
        MealInformationSection(
            timeMinutes = 780,
            onTimeClick = {},
            type = MealType.LUNCH,
            onTypeChange = {},
        )
    }
}
