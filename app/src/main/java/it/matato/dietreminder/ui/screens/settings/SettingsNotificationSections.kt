package it.matato.dietreminder.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun SettingsNotificationsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.notifications_alarms),
        icon = Icons.Rounded.Notifications,
    ) {
        SettingsListItem(
            title = stringResource(R.string.meal_reminders),
            subtitle = stringResource(R.string.meal_reminders_desc),
            leadingIcon = Icons.Rounded.Notifications,
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            },
            onClick = {
                onEnabledChange(!enabled)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsNotificationsSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsNotificationsSection(
                enabled = true,
                onEnabledChange = {},
            )
        }
    }
}