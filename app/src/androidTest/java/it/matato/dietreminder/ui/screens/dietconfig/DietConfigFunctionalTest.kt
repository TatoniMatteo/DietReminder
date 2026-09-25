package it.matato.dietreminder.ui.screens.dietconfig

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class DietConfigFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDietConfig_DisplaysMealsAndAllowsAddMeal() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        var addMealClicked = false
        var backClicked = false
        var dietId = 0L

        runBlocking {
            dietId = fakeRepository.create("Dieta Sportiva", 60)
            val meal = Meal(
                dietId = dietId,
                dayOfWeek = DayOfWeek.MONDAY,
                type = MealType.BREAKFAST,
                timeMinutes = 8 * 60,
                description = "Oatmeal"
            )
            fakeRepository.saveMeal(meal, emptyList())
        }

        composeTestRule.setContent {
            DietTheme {
                DietConfigScreen(
                    vm = viewModel,
                    dietId = dietId,
                    onBack = { backClicked = true },
                    onAddMeal = { _, _ -> addMealClicked = true },
                    onEditMeal = { _, _ -> }
                )
            }
        }

        dietConfigRobot(composeTestRule) {
            clickBack()
            assertTrue(backClicked)

            clickAddMeal()
            assertTrue(addMealClicked)
        }
    }
}
