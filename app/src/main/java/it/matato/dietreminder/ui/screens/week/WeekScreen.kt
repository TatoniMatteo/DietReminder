package it.matato.dietreminder.ui.screens.week

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import kotlinx.coroutines.launch

@Composable
fun WeekScreen(vm: DietViewModel, padding: PaddingValues, targetMealId: Long? = null, targetDayName: String? = null) {
    val active by vm.active.collectAsState()
    val meals by vm.meals.collectAsState()

    val days = DayOfWeek.entries
    val today = LocalDate.now().dayOfWeek

    val initialPage = remember {
        val targetDay = targetDayName?.let {
            try {
                DayOfWeek.valueOf(it)
            } catch (_: Exception) {
                null
            }
        } ?: today

        days.indexOf(targetDay).coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage, pageCount = { days.size })

    val scope = rememberCoroutineScope()

    LaunchedEffect(targetDayName) {
        targetDayName?.let { name ->
            try {
                val day = DayOfWeek.valueOf(name)
                val index = days.indexOf(day)

                if (index >= 0 && index != pagerState.currentPage) {
                    pagerState.animateScrollToPage(index)
                }
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WeekHeader(
                dietName = active?.name ?: stringResource(R.string.no_diet)
            )

            DaySelector(
                days = days, selectedPage = pagerState.currentPage, today = today, onDaySelected = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                })

            HorizontalPager(
                state = pagerState, modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), verticalAlignment = Alignment.Top
            ) { page ->
                val selectedDay = days[page]

                val dayMeals = remember(meals, selectedDay) {
                    meals.filter { it.meal.dayOfWeek == selectedDay }.sortedBy { it.meal.timeMinutes }
                }

                if (dayMeals.isEmpty()) {
                    EmptyDayState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp
                        ), verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = dayMeals, key = { it.meal.id }) { mealDetails ->
                            MealTimelineItem(
                                mealDetails = mealDetails, initiallyExpanded = mealDetails.meal.id == targetMealId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(dietName: String) {
    Column(
        modifier = Modifier.padding(
            start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp
        )
    ) {
        Text(
            text = stringResource(R.string.week),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = dietName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DaySelector(days: List<DayOfWeek>, selectedPage: Int, today: DayOfWeek, onDaySelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp
            ), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEachIndexed { index, day ->
            val selected = selectedPage == index
            val isToday = day == today

            DaySelectorItem(
                day = day, selected = selected, isToday = isToday, onClick = {
                    onDaySelected(index)
                })
        }
    }
}

@Composable
private fun DaySelectorItem(day: DayOfWeek, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
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
            .clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor), contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.getDisplayName(
                    TextStyle.NARROW, LocalLocale.current.platformLocale
                ).uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = contentColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .alpha(if (isToday) 1f else 0f)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun EmptyDayState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_no_meal), contentDescription = null, modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_meals_for_day),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MealTimelineItem(mealDetails: MealWithDetails, initiallyExpanded: Boolean = false) {
    var expanded by rememberSaveable(
        mealDetails.meal.id, initiallyExpanded
    ) {
        mutableStateOf(initiallyExpanded)
    }

    val mealIcon = getMealIcon(mealDetails)

    val mealLabel = mealDetails.meal.customTypeLabel ?: stringResource(mealDetails.meal.type.resId)

    val hour = mealDetails.meal.timeMinutes / 60
    val minute = mealDetails.meal.timeMinutes % 60
    val time = "%02d:%02d".format(hour, minute)

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(mealIcon), contentDescription = null, modifier = Modifier.size(19.dp)
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
                }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ), elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mealLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (mealDetails.meal.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = mealDetails.meal.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier.size(34.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Rounded.KeyboardArrowUp
                                } else {
                                    Icons.Rounded.KeyboardArrowDown
                                }, contentDescription = if (expanded) {
                                    stringResource(R.string.collapse)
                                } else {
                                    stringResource(R.string.expand)
                                }, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        mealDetails.courses.forEachIndexed { index, course ->
                            CourseSection(
                                course = course
                            )

                            if (index < mealDetails.courses.lastIndex) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseSection(course: CourseWithItems) {
    Column {
        Text(
            text = course.course.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        course.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp), verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )

                Spacer(modifier = Modifier.width(9.dp))

                Text(
                    text = buildString {
                        append(item.name)

                        if (item.quantities.isNotEmpty()) {
                            append("  ")
                            append(item.quantities.joinToString(" / "))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun getMealIcon(
    mealDetails: MealWithDetails
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
