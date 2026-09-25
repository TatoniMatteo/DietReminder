package it.matato.dietreminder.ui.screens.dietconfig

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.components.IconContainer
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun DietConfigScreen(
    vm: DietViewModel,
    dietId: Long,
    onBack: () -> Unit,
    onAddMeal: (Long, DayOfWeek) -> Unit,
    onEditMeal: (Long, DayOfWeek) -> Unit,
) {
    val diets by vm.diets.collectAsState()
    val meals by vm.meals.collectAsState()
    val mealRemindersEnabled by vm.mealRemindersEnabled.collectAsState()

    var selectedDay by rememberSaveable {
        mutableStateOf(LocalDate.now().dayOfWeek)
    }

    val diet = remember(diets, dietId) {
        diets.find { it.id == dietId }
    }

    var dietMeals by remember {
        mutableStateOf<List<MealWithDetails>>(emptyList())
    }

    LaunchedEffect(dietId, selectedDay, meals) {
        val allMeals = if (vm.active.value?.id == dietId) {
            meals
        } else {
            vm.getDietMeals(dietId)
        }

        dietMeals = allMeals.filter { it.meal.dayOfWeek == selectedDay }.sortedBy { it.meal.timeMinutes }
    }

    val isDayNotificationEnabled = diet?.isDayNotificationEnabled(selectedDay) == true && mealRemindersEnabled

    DietConfigContent(
        dietName = diet?.name ?: stringResource(R.string.configure_diet),
        selectedDay = selectedDay,
        dietMeals = dietMeals,
        isDayNotificationEnabled = isDayNotificationEnabled,
        isDayNotificationSwitchEnabled = mealRemindersEnabled,
        onToggleDayNotification = { enabled ->
            vm.setDietDayNotificationEnabled(dietId, selectedDay, enabled)
        },
        onBack = onBack,
        onDaySelected = { selectedDay = it },
        onAddMeal = { onAddMeal(dietId, selectedDay) },
        onEditMeal = { mealId -> onEditMeal(mealId, selectedDay) }
    )
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun DietConfigContent(
    dietName: String,
    selectedDay: DayOfWeek,
    dietMeals: List<MealWithDetails>,
    isDayNotificationEnabled: Boolean,
    isDayNotificationSwitchEnabled: Boolean,
    onToggleDayNotification: (Boolean) -> Unit,
    onBack: () -> Unit,
    onDaySelected: (DayOfWeek) -> Unit,
    onAddMeal: () -> Unit,
    onEditMeal: (Long) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMeal,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DietConfigHeader(
                dietName = dietName,
                onBack = onBack,
            )

            DaySelector(
                selectedDay = selectedDay,
                onDaySelected = onDaySelected,
            )

            DayNotificationHeaderCard(
                isEnabled = isDayNotificationEnabled,
                isSwitchEnabled = isDayNotificationSwitchEnabled,
                onToggle = onToggleDayNotification,
            )

            AnimatedContent(
                targetState = selectedDay,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(
                        SizeTransform(clip = false),
                    )
                },
                label = "day_change",
            ) { targetDay ->
                if (dietMeals.isEmpty()) {
                    EmptyDayState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 12.dp,
                            bottom = 28.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = dietMeals,
                            key = { it.meal.id },
                        ) { mealDetails ->
                            MealTimelineItem(
                                mealDetails = mealDetails,
                                onClick = {
                                    onEditMeal(mealDetails.meal.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayNotificationHeaderCard(
    isEnabled: Boolean,
    isSwitchEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.day_notifications),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(
                    text = if (isEnabled) stringResource(R.string.notification_enabled) else stringResource(R.string.notification_disabled),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                IconContainer(
                    icon = if (isEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff
                )
            },
            trailingContent = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    enabled = isSwitchEnabled,
                )
            }
        )
    }
}

@Composable
private fun DietConfigHeader(
    dietName: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 24.dp,
                top = 10.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }

        Column(
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.configure_diet),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = dietName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
) {
    val today = LocalDate.now().dayOfWeek

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayOfWeek.entries.forEach { day ->
            val selected = day == selectedDay
            val isToday = day == today

            DaySelectorItem(
                day = day,
                selected = selected,
                isToday = isToday,
                onClick = {
                    onDaySelected(day)
                },
            )
        }
    }
}

@Composable
private fun DaySelectorItem(
    day: DayOfWeek,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .width(42.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.getDisplayName(
                    TextStyle.NARROW,
                    LocalLocale.current.platformLocale,
                ).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .alpha(if (isToday) 1f else 0f)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun EmptyDayState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_meals_for_day),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.add_meal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MealTimelineItem(
    mealDetails: MealWithDetails,
    onClick: () -> Unit,
) {
    val mealIcon = getMealIcon(mealDetails)

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

            Spacer(modifier = Modifier.height(8.dp))

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
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 15.dp,
                    ),
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
                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = mealDetails.meal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }

                    if (mealDetails.courses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(
                                R.string.courses_count,
                                mealDetails.courses.size,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun getMealIcon(
    mealDetails: MealWithDetails,
): Int {
    return when (mealDetails.meal.type) {
        MealType.BREAKFAST -> R.drawable.ic_breakfast
        MealType.MORNING_SNACK -> R.drawable.ic_morning_snack
        MealType.LUNCH -> R.drawable.ic_lunch
        MealType.AFTERNOON_SNACK -> R.drawable.ic_afternoon_snack
        MealType.DINNER -> R.drawable.ic_dinner
        MealType.OTHER -> R.drawable.ic_other_meal
    }
}

@Preview(showBackground = true)
@Composable
private fun DietConfigContentPreview() {
    DietTheme {
        DietConfigContent(
            dietName = "Summer Diet",
            selectedDay = DayOfWeek.MONDAY,
            dietMeals = listOf(
                MealWithDetails(
                    meal = Meal(
                        id = 1,
                        dietId = 1,
                        type = MealType.BREAKFAST,
                        timeMinutes = 480,
                        dayOfWeek = DayOfWeek.MONDAY,
                        description = "Healthy breakfast"
                    ),
                    courses = emptyList()
                )
            ),
            isDayNotificationEnabled = true,
            isDayNotificationSwitchEnabled = true,
            onToggleDayNotification = {},
            onBack = {},
            onDaySelected = {},
            onAddMeal = {},
            onEditMeal = {}
        )
    }
}
