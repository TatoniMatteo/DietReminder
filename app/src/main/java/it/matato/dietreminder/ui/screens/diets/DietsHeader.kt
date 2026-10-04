package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme

@Composable
fun DietsHeader(
	count: Int,
	onImportClick: () -> Unit,
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically,
	) {
		Column(
			verticalArrangement = Arrangement.spacedBy(6.dp),
		) {
			Text(
				text = stringResource(R.string.diets),
				style = MaterialTheme.typography.headlineLarge,
				fontWeight = FontWeight.Bold,
			)

			Row(
				verticalAlignment = Alignment.CenterVertically,
			) {
				Box(
					modifier = Modifier
						.size(7.dp)
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.primary),
				)

				Spacer(modifier = Modifier.width(8.dp))

				Text(
					text = count.toString(),
					style = MaterialTheme.typography.bodyMedium,
					fontWeight = FontWeight.Medium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}

		IconButton(onClick = onImportClick) {
			Icon(
				imageVector = Icons.Rounded.FileUpload,
				contentDescription = stringResource(R.string.import_diet),
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun DietsHeaderPreview() {
	DietTheme {
		DietsHeader(
			count = 5,
			onImportClick = {},
		)
	}
}
