package it.matato.dietreminder.ui.screens.diets

import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.theme.AppSpace

@Composable
fun DietsHeader(count: Int, onImportClick: () -> Unit, isOffline: Boolean = LocalOfflineMode.current) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.inline)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.diets), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(count.toString(), Modifier.testTag("dietsCount").padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(AppSpace.inline))
            FilledTonalIconButton(onClick = onImportClick, enabled = !isOffline) {
                Icon(Icons.Rounded.FileUpload, stringResource(R.string.import_diet))
            }
        }
        Text(stringResource(R.string.diets_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
