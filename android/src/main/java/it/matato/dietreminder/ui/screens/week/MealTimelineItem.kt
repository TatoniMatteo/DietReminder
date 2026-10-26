package it.matato.dietreminder.ui.screens.week

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.resId
import it.matato.dietreminder.ui.theme.AppMotion
import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.theme.LocalCompactLayout

@Composable
fun MealTimelineItem(
    mealDetails: MealWithDetails,
    initiallyExpanded: Boolean = false,
    isHighlighted: Boolean = false,
    isCurrent: Boolean = false,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(mealDetails.meal.id, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val compact = LocalCompactLayout.current
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (isHighlighted) colors.primaryContainer else colors.surfaceContainerLow,
        tween(AppMotion.standard), label = "mealBackground",
    )
    val foreground = if (isHighlighted) colors.onPrimaryContainer else colors.onSurface
    val secondary = if (isHighlighted) colors.onPrimaryContainer else colors.onSurfaceVariant
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(AppMotion.standard), label = "mealChevron")
    val stateLabel = stringResource(if (expanded) R.string.collapse else R.string.expand)
    val icon = when (mealDetails.meal.type) {
        MealType.BREAKFAST -> R.drawable.ic_breakfast
        MealType.MORNING_SNACK -> R.drawable.ic_morning_snack
        MealType.LUNCH -> R.drawable.ic_lunch
        MealType.AFTERNOON_SNACK -> R.drawable.ic_afternoon_snack
        MealType.DINNER -> R.drawable.ic_dinner
        MealType.OTHER -> R.drawable.ic_other_meal
    }
    Card(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth().testTag("weekMeal_${mealDetails.meal.id}").semantics { stateDescription = stateLabel },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = background),
        border = if (isHighlighted) BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)) else null,
    ) {
        Column(Modifier.padding(AppSpace.card), verticalArrangement = Arrangement.spacedBy(AppSpace.item)) {
            if (isHighlighted && !compact) {
                Text(stringResource(if (isCurrent) R.string.week_current_meal else R.string.week_next_meal),
                    style = MaterialTheme.typography.labelMedium, color = foreground, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.item)) {
                Surface(shape = MaterialTheme.shapes.medium, color = if (isHighlighted) colors.surface.copy(alpha = 0.6f) else colors.secondaryContainer) {
                    Icon(painterResource(icon), null, Modifier.padding(AppSpace.iconPadding).size(24.dp), tint = if (isHighlighted) foreground else colors.onSecondaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.small)) {
                    Text(mealDetails.meal.customTypeLabel ?: stringResource(mealDetails.meal.type.resId),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = foreground)
                    if (!compact) {
                        Text("%02d:%02d".format(mealDetails.meal.timeMinutes / 60, mealDetails.meal.timeMinutes % 60),
                            style = MaterialTheme.typography.bodyMedium, color = secondary)
                    } else if (isHighlighted) {
                        Text(stringResource(if (isCurrent) R.string.week_current_meal else R.string.week_next_meal),
                            style = MaterialTheme.typography.labelMedium, color = foreground, fontWeight = FontWeight.Bold)
                    }
                }
                if (compact) {
                    Text("%02d:%02d".format(mealDetails.meal.timeMinutes / 60, mealDetails.meal.timeMinutes % 60),
                        style = MaterialTheme.typography.bodyMedium, color = secondary, maxLines = 1)
                }
                Icon(Icons.Rounded.KeyboardArrowDown, stateLabel, Modifier.rotate(rotation).size(24.dp), tint = secondary)
            }
            val preview = if (expanded) mealDetails.meal.description else mealDetails.courses.map { it.course.name.trim() }
                .filter { it.isNotBlank() }.joinToString(", ")
            if (preview.isNotBlank()) {
                Text(preview, style = MaterialTheme.typography.bodyMedium, color = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(AppMotion.standard)) + fadeIn(tween(AppMotion.quick)),
                exit = shrinkVertically(tween(AppMotion.standard)) + fadeOut(tween(AppMotion.quick)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.group)) {
                    HorizontalDivider(color = foreground.copy(alpha = 0.12f))
                    if (mealDetails.courses.isEmpty()) {
                        Text(stringResource(R.string.week_no_courses), style = MaterialTheme.typography.bodyMedium, color = secondary)
                    }
                    mealDetails.courses.forEach { course -> CourseSection(course) }
                    if (onEdit != null) {
                        TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(AppSpace.inline))
                            Text(stringResource(R.string.edit_meal))
                        }
                    }
                }
            }
        }
    }
}
