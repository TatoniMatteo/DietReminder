package it.matato.dietreminder.ui.screens.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R

@Composable
fun OfflineScreen(
	onRetryClick: () -> Unit,
	onContinueClick: () -> Unit,
) {
	Surface(
		modifier = Modifier.fillMaxSize(),
		color = MaterialTheme.colorScheme.background,
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(32.dp),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Icon(
				imageVector = Icons.Rounded.CloudOff,
				contentDescription = null,
				modifier = Modifier.size(80.dp),
				tint = MaterialTheme.colorScheme.primary,
			)

			Spacer(Modifier.height(24.dp))

			Text(
				text = stringResource(R.string.offline_title),
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onBackground,
				textAlign = TextAlign.Center,
			)

			Spacer(Modifier.height(12.dp))

			Text(
				text = stringResource(R.string.offline_message),
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
			)

			Spacer(Modifier.height(32.dp))

			Button(
				onClick = onRetryClick,
				modifier = Modifier.height(50.dp),
			) {
				Text(
					text = stringResource(R.string.retry),
					style = MaterialTheme.typography.titleMedium,
				)
			}

			Spacer(Modifier.height(12.dp))

			Button(
				onClick = onContinueClick,
				modifier = Modifier.height(50.dp),
			) {
				Text(
					text = stringResource(R.string.continue_offline),
					style = MaterialTheme.typography.titleMedium,
				)
			}
		}
	}
}
