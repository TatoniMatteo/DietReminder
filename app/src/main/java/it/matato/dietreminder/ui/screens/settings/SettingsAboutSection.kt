package it.matato.dietreminder.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Update
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun SettingsAboutSection(
    onVersionClick: () -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.app_info),
        icon = Icons.Rounded.Person,
    ) {
        SettingsListItem(
            title = stringResource(R.string.version),
            subtitle = "1.0.0 (Build 20261027)",
            leadingIcon = Icons.Rounded.Update,
            onClick = onVersionClick,
        )

        SettingsDivider()

        SettingsListItem(
            title = stringResource(R.string.developer),
            subtitle = "Matteo Tatoni",
            leadingIcon = Icons.Rounded.Person,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsAboutSectionPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsAboutSection(
                onVersionClick = {},
            )
        }
    }
}