package it.matato.dietreminder.ui.screens.mealdetail

import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.data.model.resId
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun MealInformationSection(
	timeMinutes: Int,
	onTimeClick: () -> Unit,
	type: MealType,
	onTypeChange: (MealType) -> Unit,
	isNotificationEnabled: Boolean,
	onNotificationEnabledChange: (Boolean) -> Unit,
	isNotificationSwitchEnabled: Boolean = true,
) {
	val isOffline = LocalOfflineMode.current
	var typeMenuExpanded by remember { mutableStateOf(false) }

	AppCard(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
	) {
		Column {
			// Row 1: Time
			ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
				headlineContent = {
					Text(
						text = stringResource(R.string.meal_time),
						fontWeight = FontWeight.SemiBold,
					)
				},
				leadingContent = {
					IconContainer(icon = Icons.Rounded.Schedule)
				},
				trailingContent = {
					OutlinedButton(onClick = onTimeClick, enabled = !isOffline) {
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
				modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpace.screen),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
			)

			// Row 2: Meal Type
			Box {
				ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
					modifier = Modifier.clickable(enabled = !isOffline) {
						typeMenuExpanded = true
					},
					headlineContent = {
						Text(
							text = stringResource(R.string.meal_type),
							fontWeight = FontWeight.SemiBold,
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
							enabled = !isOffline,
						)
					}
				}
			}

			HorizontalDivider(
				modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpace.screen),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
			)

			// Row 3: Meal Notifications
			ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
				headlineContent = {
					Text(
						text = stringResource(R.string.meal_notifications),
						fontWeight = FontWeight.SemiBold,
					)
				},
				supportingContent = {
					Text(
						text = if (isNotificationEnabled) {
							stringResource(R.string.notification_enabled)
						} else {
							stringResource(R.string.notification_disabled)
						},
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				},
				leadingContent = {
					IconContainer(
						icon = if (isNotificationEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff
					)
				},
				trailingContent = {
					Switch(
						checked = isNotificationEnabled,
						onCheckedChange = onNotificationEnabledChange,
						enabled = isNotificationSwitchEnabled && !isOffline,
						modifier = Modifier.testTag("meal-notifications-switch"),
					)
				}
			)
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
			isNotificationEnabled = true,
			onNotificationEnabledChange = {},
		)
	}
}
