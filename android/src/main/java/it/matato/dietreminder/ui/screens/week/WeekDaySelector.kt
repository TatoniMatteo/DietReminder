package it.matato.dietreminder.ui.screens.week

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.ui.theme.AppMotion
import it.matato.dietreminder.ui.theme.AppSpace
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun WeekDaySelector(days: List<DayOfWeek>, selectedPage: Int, today: DayOfWeek, onDaySelected: (Int) -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val monday = LocalDate.now().with(DayOfWeek.MONDAY)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AppSpace.group).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.small),
    ) {
        days.forEachIndexed { index, day ->
            val selected = index == selectedPage
            val isToday = day == today
            val date = monday.plusDays((day.value - 1).toLong())
            val background by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                tween(AppMotion.quick), label = "dayBackground",
            )
            val foreground by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(AppMotion.quick), label = "dayForeground",
            )
            Column(
                Modifier.weight(1f).clip(MaterialTheme.shapes.large).background(background)
                    .selectable(selected, onClick = { onDaySelected(index) }, role = Role.Tab)
                    .semantics { contentDescription = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)) }
                    .padding(vertical = AppSpace.item),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpace.small),
            ) {
                Text(
                    day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = foreground
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = foreground,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    Modifier.size(4.dp).clip(MaterialTheme.shapes.small).background(
                        if (isToday) foreground else androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        }
    }
}
