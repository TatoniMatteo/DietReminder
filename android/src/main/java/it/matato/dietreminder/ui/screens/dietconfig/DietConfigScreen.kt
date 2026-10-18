package it.matato.dietreminder.ui.screens.dietconfig

import it.matato.dietreminder.ui.theme.AppSpace
import it.matato.dietreminder.ui.components.AppScaffold
import it.matato.dietreminder.ui.components.AppCard
import it.matato.dietreminder.data.model.resId
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
import it.matato.dietreminder.ui.icons.AppIcons as Icons
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.tween
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import it.matato.dietreminder.ui.screens.week.WeekDaySelector
import it.matato.dietreminder.ui.theme.AppMotion
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.LocalOfflineMode
import it.matato.dietreminder.ui.OfflineAwareFloatingActionButton
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
    onIngredientsClick: () -> Unit,
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
        onIngredientsClick = onIngredientsClick,
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
    onIngredientsClick: () -> Unit,
    onDaySelected: (DayOfWeek) -> Unit,
    onAddMeal: () -> Unit,
    onEditMeal: (Long) -> Unit,
) {
    val isOffline = LocalOfflineMode.current
    AppScaffold(
        floatingActionButton = {
            OfflineAwareFloatingActionButton(
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
                onIngredientsClick = onIngredientsClick,
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
                        slideInHorizontally(tween(AppMotion.standard)) { it / 8 } + fadeIn() togetherWith slideOutHorizontally(
                            tween(AppMotion.standard)
                        ) { -it / 8 } + fadeOut()
                    } else {
                        slideInHorizontally(tween(AppMotion.standard)) { -it / 8 } + fadeIn() togetherWith slideOutHorizontally(
                            tween(AppMotion.standard)
                        ) { it / 8 } + fadeOut()
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
                            start = AppSpace.screen,
                            end = AppSpace.screen,
                            top = 12.dp,
                            bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppSpace.group),
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
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.screen, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                    enabled = isSwitchEnabled && !LocalOfflineMode.current,
                    modifier = Modifier.testTag("diet-day-notifications-switch"),
                )
            }
        )
    }
}

@Composable
private fun DietConfigHeader(dietName: String, onBack: () -> Unit, onIngredientsClick: () -> Unit) {
    it.matato.dietreminder.ui.components.AppTopBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.small)) {
                Text(
                    stringResource(R.string.configure_diet),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(dietName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2)
            }
        },
        navigationIcon = {
            FilledTonalIconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    stringResource(R.string.back)
                )
            }
        },
        actions = {
            FilledTonalIconButton(onClick = onIngredientsClick) {
                Icon(
                    Icons.Rounded.Kitchen,
                    stringResource(R.string.ingredients_title)
                )
            }
        },
    )
}

@Composable
private fun DaySelector(selectedDay: DayOfWeek, onDaySelected: (DayOfWeek) -> Unit) {
    WeekDaySelector(
        DayOfWeek.entries, selectedDay.ordinal, LocalDate.now().dayOfWeek,
        onDaySelected = { onDaySelected(DayOfWeek.entries[it]) })
    Spacer(Modifier.height(AppSpace.item))
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
private fun MealTimelineItem(mealDetails: MealWithDetails, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(AppSpace.card), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.item)
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(
                    painterResource(getMealIcon(mealDetails)),
                    null,
                    Modifier.padding(AppSpace.iconPadding).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.small)) {
                Text(
                    mealDetails.meal.customTypeLabel ?: stringResource(mealDetails.meal.type.resId),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
                Text(
                    "%02d:%02d".format(mealDetails.meal.timeMinutes / 60, mealDetails.meal.timeMinutes % 60),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mealDetails.meal.description.isNotBlank()) {
                    Text(
                        mealDetails.meal.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2
                    )
                }
                if (mealDetails.courses.isNotEmpty()) {
                    Text(
                        pluralStringResource(
                            R.plurals.courses_count,
                            mealDetails.courses.size,
                            mealDetails.courses.size
                        ),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            onIngredientsClick = {},
            onDaySelected = {},
            onAddMeal = {},
            onEditMeal = {}
        )
    }
}
