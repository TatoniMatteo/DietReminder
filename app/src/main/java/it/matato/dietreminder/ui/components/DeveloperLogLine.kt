package it.matato.dietreminder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.util.LogEntry
import it.matato.dietreminder.util.LogLevel

@Composable
fun DeveloperLogLine(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.TRACE -> MaterialTheme.colorScheme.outline
    }

    Text(
        text = "[${entry.timestamp}] ${entry.level.name.take(1)}: ${entry.message}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
private fun DeveloperLogLinePreview() {
    DietTheme {
        Column {
            DeveloperLogLine(
                entry = LogEntry(
                    timestamp = "12:00:00",
                    level = LogLevel.INFO,
                    message = "Information message",
                ),
            )
            DeveloperLogLine(
                entry = LogEntry(
                    timestamp = "12:00:01",
                    level = LogLevel.ERROR,
                    message = "Error message",
                ),
            )
        }
    }
}
