package it.matato.dietreminder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import it.matato.dietreminder.util.LogEntry
import it.matato.dietreminder.util.LogLevel

@Composable
fun DeveloperLogLine(entry: LogEntry, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(entry.timestamp, entry.level, entry.message) { mutableStateOf(false) }
    var overflowing by remember(entry) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val levelColor = when (entry.level) {
        LogLevel.ERROR -> colors.error
        LogLevel.WARN -> colors.tertiary
        LogLevel.INFO -> colors.primary
        LogLevel.DEBUG, LogLevel.TRACE -> colors.onSurfaceVariant
    }
    Column(modifier.fillMaxWidth().clickable(enabled = overflowing || expanded) { expanded = !expanded }
        .padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.timestamp, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = colors.onSurfaceVariant)
            Text(entry.level.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = levelColor,
                modifier = Modifier.weight(1f))
            if (overflowing || expanded) Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                stringResource(if (expanded) R.string.collapse else R.string.expand), Modifier.size(16.dp), tint = colors.onSurfaceVariant)
        }
        if (expanded) SelectionContainer {
            Text(entry.message, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
        } else Text(entry.message, style = MaterialTheme.typography.bodySmall, color = colors.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis, onTextLayout = { overflowing = it.hasVisualOverflow })
    }
}
