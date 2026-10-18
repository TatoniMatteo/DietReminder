package it.matato.dietreminder.ui.screens.week

import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.AppSpace

@Composable
fun WeekHeader(dietName: String?, onToday: (() -> Unit)? = null) {
    Column(Modifier.padding(horizontal = AppSpace.screen).padding(top = AppSpace.group, bottom = AppSpace.group)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.brand_logo), null, Modifier.size(38.dp))
            Spacer(Modifier.width(AppSpace.inline))
            Text(stringResource(R.string.week), Modifier.weight(1f).testTag("weekTitle"), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
            if (onToday != null) {
                FilledTonalButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.heightIn(min = 48.dp).testTag("weekToday")) {
                    Icon(Icons.Rounded.Today, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpace.inline))
                    Text(stringResource(R.string.week_today))
                }
            }
        }
        Spacer(Modifier.height(AppSpace.item))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.inline)) {
            Icon(Icons.Rounded.Restaurant, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text(dietName ?: stringResource(R.string.no_diet), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
