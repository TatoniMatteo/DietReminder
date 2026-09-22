package it.matato.dietreminder.ui.screens.week

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.theme.DietTheme
import java.time.DayOfWeek

@Composable
fun MealTimelineItem(
    mealDetails: MealWithDetails,
    initiallyExpanded: Boolean = false,
) {
    var expanded by rememberSaveable(
        mealDetails.meal.id,
        initiallyExpanded,
    ) {
        mutableStateOf(initiallyExpanded)
    }

    val mealIcon = when (mealDetails.meal.type) {
        MealType.BREAKFAST -> R.drawable.ic_breakfast
        MealType.MORNING_SNACK -> R.drawable.ic_morning_snack
        MealType.LUNCH -> R.drawable.ic_lunch
        MealType.AFTERNOON_SNACK -> R.drawable.ic_afternoon_snack
        MealType.DINNER -> R.drawable.ic_dinner
        MealType.OTHER -> R.drawable.ic_other_meal
    }

    val mealLabel = mealDetails.meal.customTypeLabel ?: stringResource(mealDetails.meal.type.resId)

    val hour = mealDetails.meal.timeMinutes / 60
    val minute = mealDetails.meal.timeMinutes % 60
    val time = "%02d:%02d".format(hour, minute)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(58.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.size(8.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(mealIcon),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .animateContentSize()
                .clickable {
                    expanded = !expanded
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = mealLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (mealDetails.meal.description.isNotBlank()) {
                            Spacer(modifier = Modifier.size(3.dp))

                            Text(
                                text = mealDetails.meal.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Rounded.KeyboardArrowUp
                                } else {
                                    Icons.Rounded.KeyboardArrowDown
                                },
                                contentDescription = if (expanded) {
                                    stringResource(R.string.collapse)
                                } else {
                                    stringResource(R.string.expand)
                                },
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(modifier = Modifier.size(14.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        Spacer(modifier = Modifier.size(14.dp))

                        mealDetails.courses.forEachIndexed { index, course ->
                            CourseSection(course = course)

                            if (index < mealDetails.courses.lastIndex) {
                                Spacer(modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealTimelineItemPreview() {
    DietTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MealTimelineItem(
                mealDetails = MealWithDetails(
                    meal = Meal(
                        id = 1,
                        dietId = 1,
                        type = MealType.BREAKFAST,
                        timeMinutes = 480,
                        dayOfWeek = DayOfWeek.MONDAY,
                        description = "Healthy breakfast"
                    ),
                    courses = emptyList()
                ),
                initiallyExpanded = true
            )
        }
    }
}