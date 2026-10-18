package it.matato.dietreminder.ui.screens.developer

import it.matato.dietreminder.ui.theme.AppSpace
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DeveloperNotificationSection(
	onScheduledTrigger: () -> Unit,
) {
	Column(
		verticalArrangement = Arrangement.spacedBy(AppSpace.item),
	) {
		SectionTitle(
			title = stringResource(R.string.notification_tests),
			icon = Icons.Rounded.NotificationsActive,
		)

		Column(
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			OutlinedButton(
				onClick = onScheduledTrigger,
				modifier = Modifier.fillMaxWidth(),
			) {
				Icon(
					imageVector = Icons.Rounded.Timer,
					contentDescription = null,
				)

				Spacer(modifier = Modifier.width(8.dp))

				Text(stringResource(R.string.test_scheduler))
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun DeveloperNotificationSectionPreview() {
	DietTheme {
		Column(modifier = Modifier.padding(AppSpace.card)) {
			DeveloperNotificationSection(
				onScheduledTrigger = {},
			)
		}
	}
}