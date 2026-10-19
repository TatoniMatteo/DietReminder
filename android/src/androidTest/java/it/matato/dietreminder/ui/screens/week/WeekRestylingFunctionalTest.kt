package it.matato.dietreminder.ui.screens.week

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.theme.DietTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class WeekRestylingFunctionalTest {
    @get:Rule
    val compose = createComposeRule()
    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
    private val lunch = MealWithDetails(
        Meal(
            id = 21,
            dietId = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.LUNCH,
            timeMinutes = 780,
            description = "Pranzo di prova"
        ),
        listOf(
            CourseWithItems(
                Course(id = 1, mealId = 21, name = "Piatto unico"),
                listOf(FoodItem(id = 1, courseId = 1, name = "Riso", amount = "80"))
            )
        ),
    )

    @Test
    fun mealExpandsAndOpensTheEditor() {
        var edited = false
        compose.setContent {
            DietTheme {
                MealTimelineItem(lunch, onEdit = { edited = true })
            }
        }
        compose.onNodeWithText("Piatto unico").assertDoesNotExist()
        compose.onNodeWithTag("weekMeal_21").performClick()
        compose.onNodeWithText("Piatto unico").assertIsDisplayed()
        compose.onNodeWithText("Riso").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.edit_meal)).performClick()
        compose.runOnIdle { assertTrue(edited) }
        compose.onNodeWithTag("weekMeal_21").performClick()
        compose.onNodeWithText("Piatto unico").assertDoesNotExist()
    }

    @Test
    fun nextMealHighlightStaysOnTodayAndTodayButtonReturnsToIt() {
        compose.setContent {
            val pager = rememberPagerState(pageCount = { 7 })
            val scope = rememberCoroutineScope()
            DietTheme {
                WeekContent(
                    dietName = "Dieta prova",
                    meals = listOf(lunch, lunch.copy(meal = lunch.meal.copy(id = 22, dayOfWeek = DayOfWeek.TUESDAY))),
                    targetMealId = null,
                    days = DayOfWeek.entries,
                    today = DayOfWeek.MONDAY,
                    pagerState = pager,
                    onDaySelected = { scope.launch { pager.animateScrollToPage(it) } },
                    now = LocalDateTime.of(2026, 10, 12, 12, 0),
                )
            }
        }
        compose.onNodeWithText(text(R.string.week_next_meal)).assertIsDisplayed()
        compose.onNodeWithTag("weekPager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("weekMeal_22").assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.week_next_meal)).assertCountEquals(0)
        compose.onNodeWithTag("weekToday").performClick()
        compose.onNodeWithTag("weekMeal_21").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.week_next_meal)).assertIsDisplayed()
    }
}
