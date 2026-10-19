package it.matato.dietreminder.ui.screens.settings

import it.matato.dietreminder.ui.theme.AppSpace
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.BuildConfig
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
        androidx.compose.foundation.layout.Row(
            Modifier.padding(horizontal = AppSpace.card, vertical = AppSpace.group),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.brand_logo), null, Modifier.then(Modifier.size(40.dp)))
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            androidx.compose.material3.Text(stringResource(R.string.app_name), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        }
		SettingsListItem(
			title = stringResource(R.string.version),
			subtitle = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
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
		Column(modifier = Modifier.padding(AppSpace.card)) {
			SettingsAboutSection(
				onVersionClick = {},
			)
		}
	}
}
