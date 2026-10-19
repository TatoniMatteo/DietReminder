package it.matato.dietreminder.ui.screens.week

import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.AppSpace

@Composable
fun EmptyDayState(hasDiet: Boolean = true, onManageDiet: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AppSpace.screen, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpace.group),
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(Icons.Rounded.CalendarToday, null, Modifier.padding(AppSpace.card).size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(stringResource(if (hasDiet) R.string.no_meals_for_day else R.string.week_start_title),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(stringResource(if (hasDiet) R.string.week_empty_description else R.string.week_no_diet_description),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (onManageDiet != null) {
            FilledTonalButton(onClick = onManageDiet) {
                Text(stringResource(if (hasDiet) R.string.week_organize_day else R.string.week_choose_diet))
            }
        }
    }
}
