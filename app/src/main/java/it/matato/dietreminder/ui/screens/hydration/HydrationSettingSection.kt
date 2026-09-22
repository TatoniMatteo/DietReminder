package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun HydrationSettingsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(
            title = stringResource(R.string.hydration_reminders),
            icon = Icons.Rounded.WaterDrop,
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.hydration_reminders),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.hydration_reminders_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    IconContainer(
                        icon = Icons.Rounded.WaterDrop,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HydrationSettingsSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HydrationSettingsSection(
                enabled = true,
                onEnabledChange = {},
            )
        }
    }
}