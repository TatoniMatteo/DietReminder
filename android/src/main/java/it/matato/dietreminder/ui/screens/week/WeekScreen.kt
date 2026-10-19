package it.matato.dietreminder.ui.screens.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import it.matato.dietreminder.R
import it.matato.dietreminder.domain.nextMeal
import it.matato.dietreminder.ui.theme.AppSpace
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun WeekScreen(
	vm: DietViewModel,
	targetMealId: Long? = null,
	targetDayName: String? = null,
	onOpenDiet: ((Long?) -> Unit)? = null,
	onEditMeal: ((MealWithDetails) -> Unit)? = null,
) {
	val active by vm.active.collectAsState()
	val meals by vm.meals.collectAsState()

	val days = DayOfWeek.entries
	val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(30_000)
        }
    }
    val today = now.dayOfWeek

	val initialPage = remember {
		val targetDay = targetDayName
			?.let { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }
			?: today

		days.indexOf(targetDay).coerceAtLeast(0)
	}

	val pagerState = rememberPagerState(
		initialPage = initialPage,
		pageCount = { days.size },
	)

	val scope = rememberCoroutineScope()

	LaunchedEffect(targetDayName) {
		val targetDay = targetDayName
			?.let { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }
			?: return@LaunchedEffect

		val index = days.indexOf(targetDay)
		if (index >= 0 && index != pagerState.currentPage) {
			pagerState.animateScrollToPage(index)
		}
	}

	WeekContent(
		dietName = active?.name,
        now = now,
        mealWindowMinutes = active?.nextMealWindowMinutes ?: 90,
        onManageDiet = onOpenDiet?.let { open -> { open(active?.id) } },
        onEditMeal = onEditMeal,
		meals = meals,
		targetMealId = targetMealId,
		days = days,
		today = today,
		pagerState = pagerState,
		onDaySelected = { index ->
			scope.launch {
				pagerState.animateScrollToPage(index)
			}
		},
	)
}

@Composable
fun WeekContent(
    dietName: String?,
    meals: List<MealWithDetails>,
    targetMealId: Long?,
    days: List<DayOfWeek>,
    today: DayOfWeek,
    pagerState: PagerState,
    onDaySelected: (Int) -> Unit,
    now: LocalDateTime = LocalDateTime.now(),
    mealWindowMinutes: Int = 90,
    onManageDiet: (() -> Unit)? = null,
    onEditMeal: ((MealWithDetails) -> Unit)? = null,
) {
    val locale = LocalLocale.current.platformLocale
    val next = remember(meals, now, mealWindowMinutes) { nextMeal(meals, now, mealWindowMinutes) }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            WeekHeader(dietName, onToday = { onDaySelected(days.indexOf(today).coerceAtLeast(0)) })
            WeekDaySelector(days, pagerState.currentPage, today, onDaySelected)
            Spacer(Modifier.height(AppSpace.inline))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("weekPager"),
                verticalAlignment = Alignment.Top,
            ) { page ->
                val selectedDay = days[page]
                val date = now.toLocalDate().with(selectedDay)
                val dayMeals = remember(meals, selectedDay) {
                    meals.filter { it.meal.dayOfWeek == selectedDay }.sortedBy { it.meal.timeMinutes }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = AppSpace.screen, vertical = AppSpace.group),
                    verticalArrangement = Arrangement.spacedBy(AppSpace.item),
                ) {
                    item(key = "dayHeading") {
                        Row(Modifier.fillMaxWidth().padding(bottom = AppSpace.small), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)).replaceFirstChar { it.titlecase(locale) },
                                Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            )
                            Text(pluralStringResource(R.plurals.week_meal_count, dayMeals.size, dayMeals.size),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (dayMeals.isEmpty()) {
                        item(key = "empty") { EmptyDayState(hasDiet = dietName != null, onManageDiet = onManageDiet) }
                    }
                    items(items = dayMeals, key = { it.meal.id }) { details ->
                        val highlighted = selectedDay == now.dayOfWeek && next?.dateTime?.toLocalDate() == now.toLocalDate() && next?.meal?.meal?.id == details.meal.id
                        MealTimelineItem(
                            mealDetails = details,
                            initiallyExpanded = details.meal.id == targetMealId,
                            isHighlighted = highlighted,
                            isCurrent = highlighted && next!!.timeRemainingMinutes <= 0,
                            onEdit = onEditMeal?.let { edit -> { edit(details) } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeekContentPreview() {
	DietTheme {
		WeekContent(
			dietName = "Summer Diet",
			meals = emptyList(),
			targetMealId = null,
			days = DayOfWeek.entries,
			today = DayOfWeek.MONDAY,
			pagerState = rememberPagerState(pageCount = { 7 }),
			onDaySelected = {}
		)
	}
}