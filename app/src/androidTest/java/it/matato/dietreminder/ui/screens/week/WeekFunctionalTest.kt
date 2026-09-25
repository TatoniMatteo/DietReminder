package it.matato.dietreminder.ui.screens.week

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.repository.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class WeekFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWeek_DisplaysEmptyStateWhenNoActiveDiet() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        composeTestRule.setContent {
            DietTheme {
                WeekScreen(vm = viewModel)
            }
        }

        weekRobot(composeTestRule) {
            verifyEmptyState()
        }
    }

    @Test
    fun testWeek_DisplaysMealsForActiveDiet() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        runBlocking {
            val dietId = fakeRepository.create("Dieta Attiva", 60)
            val meal = Meal(
                dietId = dietId,
                dayOfWeek = DayOfWeek.MONDAY,
                type = MealType.LUNCH,
                timeMinutes = 13 * 60,
                description = "Pasta al pomodoro"
            )
            fakeRepository.saveMeal(meal, emptyList())
        }

        composeTestRule.setContent {
            DietTheme {
                WeekScreen(vm = viewModel, targetDayName = "MONDAY")
            }
        }

        weekRobot(composeTestRule) {
            verifyMealVisible("Pasta al pomodoro")
        }
    }
}
