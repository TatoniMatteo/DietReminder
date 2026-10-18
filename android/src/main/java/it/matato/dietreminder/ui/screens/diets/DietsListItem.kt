package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import it.matato.dietreminder.ui.theme.AppMotion
import it.matato.dietreminder.ui.theme.AppSpace
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DietsListItem(
	diet: Diet,
	onActivate: () -> Unit,
	onDuplicate: () -> Unit,
	onDelete: () -> Unit,
	onConfigure: () -> Unit,
	isOffline: Boolean = LocalOfflineMode.current,
) {
	var menuExpanded by rememberSaveable(diet.id) {
		mutableStateOf(false)
	}

	val backgroundColor = if (diet.isActive) {
		MaterialTheme.colorScheme.primaryContainer
	} else {
		MaterialTheme.colorScheme.surfaceContainerLow
	}

    val animatedBackground by animateColorAsState(backgroundColor, tween(AppMotion.standard), label = "activeDiet")
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.background(animatedBackground),
	) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            modifier = Modifier.clickable(onClick = onConfigure).padding(vertical = AppSpace.row),
			headlineContent = {
				Text(
					text = diet.name,
					fontWeight = FontWeight.SemiBold,
					maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
				)
			},
			supportingContent = {
                Text(
                    text = if (diet.isActive) stringResource(R.string.diet_active_badge)
                    else stringResource(R.string.window_minutes_value, diet.nextMealWindowMinutes),
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			},
			leadingContent = {
				Box(
					modifier = Modifier
						.size(44.dp)
						.clip(MaterialTheme.shapes.medium)
						.background(
							if (diet.isActive) {
								MaterialTheme.colorScheme.primary
							} else {
								MaterialTheme.colorScheme.primaryContainer
							},
						),
					contentAlignment = Alignment.Center,
				) {
					Icon(
						imageVector = if (diet.isActive) {
							Icons.Rounded.Check
						} else {
							Icons.Rounded.Restaurant
						},
						contentDescription = null,
						tint = if (diet.isActive) {
							MaterialTheme.colorScheme.onPrimary
						} else {
							MaterialTheme.colorScheme.onPrimaryContainer
						},
						modifier = Modifier.size(21.dp),
					)
				}
			},
			trailingContent = {
				Box {
					IconButton(
						onClick = { menuExpanded = true },
					) {
						Icon(
							imageVector = Icons.Rounded.MoreVert,
							contentDescription = stringResource(R.string.configure),
						)
					}

					DietsActionsMenu(
						expanded = menuExpanded,
						isActive = diet.isActive,
						onDismiss = { menuExpanded = false },
						onActivate = {
							menuExpanded = false
							onActivate()
						},
						onConfigure = {
							menuExpanded = false
							onConfigure()
						},
						onDuplicate = {
							menuExpanded = false
							onDuplicate()
						},
						onDelete = {
							menuExpanded = false
							onDelete()
						},
						isOffline = isOffline,
					)
				}
			},
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun DietsListItemPreview() {
	DietTheme {
		DietsListItem(
			diet = Diet(id = 1, name = "Summer Diet", nextMealWindowMinutes = 90, isActive = true),
			onActivate = {},
			onDuplicate = {},
			onDelete = {},
			onConfigure = {},
		)
	}
}