package it.matato.dietreminder.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.AppVersionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
	state: AppVersionState,
	onUpdateClick: () -> Unit,
	onDismiss: () -> Unit,
) {
	val title = when (state) {
		AppVersionState.OBSOLETE -> stringResource(R.string.update_obsolete_title)
		AppVersionState.DEPRECATED -> stringResource(R.string.update_deprecated_title)
		else -> stringResource(R.string.update_recent_title)
	}

	val message = when (state) {
		AppVersionState.OBSOLETE -> stringResource(R.string.update_obsolete_message)
		AppVersionState.DEPRECATED -> stringResource(R.string.update_deprecated_message)
		else -> stringResource(R.string.update_recent_message)
	}

	val icon = if (state == AppVersionState.OBSOLETE) Icons.Rounded.Warning else Icons.Rounded.SystemUpdate
	val iconTint =
		if (state == AppVersionState.OBSOLETE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

	BasicAlertDialog(
		onDismissRequest = onDismiss,
	) {
		ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge,
		) {
			Column(
				modifier = Modifier.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = iconTint,
					modifier = Modifier.padding(top = 8.dp),
				)

				Text(
					text = title,
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.Bold,
				)

				Text(
					text = message,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp),
					horizontalArrangement = Arrangement.End,
				) {
					TextButton(onClick = onDismiss) {
						Text(stringResource(R.string.update_later))
					}

					Button(onClick = onUpdateClick) {
						Text(stringResource(R.string.update_now))
					}
				}
			}
		}
	}
}
