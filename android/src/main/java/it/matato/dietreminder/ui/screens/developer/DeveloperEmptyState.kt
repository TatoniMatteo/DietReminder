package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.ui.theme.AppSpace

@Composable
internal fun DeveloperEmptyState(title: String, description: String, icon: ImageVector) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppSpace.card), verticalArrangement = Arrangement.spacedBy(AppSpace.item)) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
