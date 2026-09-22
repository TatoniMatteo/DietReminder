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
import androidx.compose.material3.Scaffold
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
) {
    val active by vm.active.collectAsState()
    val meals by vm.meals.collectAsState()

    val days = DayOfWeek.entries
    val today = LocalDate.now().dayOfWeek

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
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WeekHeader(
                dietName = dietName,
            )

            WeekDaySelector(
                days = days,
                selectedPage = pagerState.currentPage,
                today = today,
                onDaySelected = onDaySelected,
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top,
            ) { page ->
                val selectedDay = days[page]

                val dayMeals = remember(meals, selectedDay) {
                    meals
                        .filter { it.meal.dayOfWeek == selectedDay }
                        .sortedBy { it.meal.timeMinutes }
                }

                if (dayMeals.isEmpty()) {
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
                            items = dayMeals,
                            key = { it.meal.id },
                        ) { mealDetails ->
                            MealTimelineItem(
                                mealDetails = mealDetails,
                                initiallyExpanded = mealDetails.meal.id == targetMealId,
                            )
                        }
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