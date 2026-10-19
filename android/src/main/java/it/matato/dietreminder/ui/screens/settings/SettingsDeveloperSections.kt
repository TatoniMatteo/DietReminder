package it.matato.dietreminder.ui.screens.settings

import it.matato.dietreminder.ui.theme.AppSpace
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun SettingsDeveloperSection(
	onNavigateToDeveloper: () -> Unit,
) {
	SettingsSection(
		title = stringResource(R.string.developer_settings),
		icon = Icons.Rounded.DeveloperMode,
	) {
		SettingsListItem(
			title = stringResource(R.string.developer_settings),
			subtitle = stringResource(R.string.developer_settings_description),
			leadingIcon = Icons.Rounded.SettingsApplications,
			onClick = onNavigateToDeveloper,
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun SettingsDeveloperSectionPreview() {
	DietTheme {
		Column(modifier = Modifier.padding(AppSpace.card)) {
			SettingsDeveloperSection(
				onNavigateToDeveloper = {},
			)
		}
	}
}