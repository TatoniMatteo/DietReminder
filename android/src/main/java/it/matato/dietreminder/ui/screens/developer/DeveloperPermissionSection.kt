package it.matato.dietreminder.ui.screens.developer

import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.components.AppCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.components.SectionTitle
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DeveloperPermissionsSection(
	onCheckPermissions: () -> Unit,
) {
	Column(
		verticalArrangement = Arrangement.spacedBy(AppSpace.item),
	) {
		SectionTitle(
			title = stringResource(R.string.developer_settings),
			icon = Icons.Rounded.Security,
		)

		AppCard(
			modifier = Modifier.fillMaxWidth(),
		) {
			Column(
				modifier = Modifier.padding(AppSpace.card),
				verticalArrangement = Arrangement.spacedBy(AppSpace.group),
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(AppSpace.group),
				) {
					Icon(
						imageVector = Icons.Rounded.Security,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
					)

					Column(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(4.dp),
					) {
						Text(
							text = stringResource(R.string.check_app_permissions),
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.SemiBold,
						)

						Text(
							text = stringResource(R.string.check_app_permissions_description),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}

				Button(
					onClick = onCheckPermissions,
					modifier = Modifier.fillMaxWidth(),
				) {
					Icon(
						imageVector = Icons.Rounded.Security,
						contentDescription = null,
					)

					Spacer(modifier = Modifier.width(8.dp))

					Text(stringResource(R.string.check_app_permissions))
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun DeveloperPermissionsSectionPreview() {
	DietTheme {
		Column(modifier = Modifier.padding(AppSpace.card)) {
			DeveloperPermissionsSection(
				onCheckPermissions = {},
			)
		}
	}
}