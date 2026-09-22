package it.matato.dietreminder.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.MealDefaultTime
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel

@Composable
fun SettingsPlanningSection(
    defaultTimes: List<MealDefaultTime>,
    vm: DietViewModel,
    onMealTypeClick: (MealType) -> Unit,
) {
    SettingsPlanningSectionContent(
        defaultTimes = defaultTimes,
        onMealTypeClick = onMealTypeClick,
        getFallbackTime = { vm.getDefaultFallback(it) }
    )
}

@Composable
fun SettingsPlanningSectionContent(
    defaultTimes: List<MealDefaultTime>,
    onMealTypeClick: (MealType) -> Unit,
    getFallbackTime: (MealType) -> Int,
) {
    SettingsSection(
        title = stringResource(R.string.planning),
        icon = Icons.Rounded.AccessTime,
    ) {
        MealType.entries.forEachIndexed { index, type ->
            if (index > 0) {
                SettingsDivider()
            }

            val timeMinutes = defaultTimes
                .find { it.type == type }
                ?.timeMinutes
                ?: getFallbackTime(type)

            SettingsListItem(
                title = stringResource(type.resId),
                subtitle = stringResource(R.string.default_time),
                leadingIcon = Icons.Rounded.AccessTime,
                trailingContent = {
                    Text(
                        text = "%02d:%02d".format(
                            timeMinutes / 60,
                            timeMinutes % 60,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                onClick = {
                    onMealTypeClick(type)
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPlanningSectionContentPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsPlanningSectionContent(
                defaultTimes = listOf(
                    MealDefaultTime(MealType.BREAKFAST, 480),
                ),
                onMealTypeClick = {},
                getFallbackTime = { 0 }
            )
        }
    }
}