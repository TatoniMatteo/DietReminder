package it.matato.dietreminder.ui.screens.mealdetail

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.repository.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class MealDetailFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMealDetail_CreateAndSaveNewMeal() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        var backClicked = false
        var dietId = 0L

        runBlocking {
            dietId = fakeRepository.create("Dieta Test", 60)
        }

        composeTestRule.setContent {
            DietTheme {
                MealDetailScreen(
                    vm = viewModel,
                    mealId = 0L,
                    dietId = dietId,
                    dayOfWeek = DayOfWeek.MONDAY,
                    onBack = { backClicked = true }
                )
            }
        }

        mealDetailRobot(composeTestRule) {
            clickSave()
            assertTrue(backClicked)
        }

        runBlocking {
            val meals = fakeRepository.getMeals(dietId)
            assertEquals(1, meals.size)
        }
    }
}
