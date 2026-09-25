package it.matato.dietreminder.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.entity.Diet
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.repository.FakeDietRepository
import it.matato.dietreminder.viewmodel.DietViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class MealNotificationGranularityTest {

    @Test
    fun diet_dayNotificationToggle_worksCorrectly() {
        var diet = Diet(name = "Dieta Test")

        assertTrue(diet.isDayNotificationEnabled(DayOfWeek.MONDAY))
        assertTrue(diet.isDayNotificationEnabled(DayOfWeek.SUNDAY))

        diet = diet.withDayNotificationToggled(DayOfWeek.SUNDAY, enabled = false)
        assertTrue(diet.isDayNotificationEnabled(DayOfWeek.MONDAY))
        assertFalse(diet.isDayNotificationEnabled(DayOfWeek.SUNDAY))

        diet = diet.withDayNotificationToggled(DayOfWeek.SUNDAY, enabled = true)
        assertTrue(diet.isDayNotificationEnabled(DayOfWeek.SUNDAY))
    }

    @Test
    fun viewModel_setDietDayNotificationEnabled_updatesRepository() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        backgroundScope.launch { viewModel.diets.collect {} }

        val dietId = fakeRepository.create("Dieta Mediterranea", 60)
        advanceUntilIdle()

        viewModel.setDietDayNotificationEnabled(dietId, DayOfWeek.FRIDAY, false)
        advanceUntilIdle()

        val updatedDiet = viewModel.diets.value.find { it.id == dietId }
        assertNotNull(updatedDiet)
        assertFalse(updatedDiet!!.isDayNotificationEnabled(DayOfWeek.FRIDAY))
        assertTrue(updatedDiet.isDayNotificationEnabled(DayOfWeek.MONDAY))
    }

    @Test
    fun meal_isNotificationEnabled_persistedCorrectly() = runBlocking {
        val fakeRepository = FakeDietRepository()
        val dietId = fakeRepository.create("Dieta Proteica", 60)

        val mealWithNotification = Meal(
            dietId = dietId,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.LUNCH,
            timeMinutes = 13 * 60,
            isNotificationEnabled = true
        )

        val mealWithoutNotification = Meal(
            dietId = dietId,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.DINNER,
            timeMinutes = 20 * 60,
            isNotificationEnabled = false
        )

        fakeRepository.saveMeal(mealWithNotification, emptyList())
        fakeRepository.saveMeal(mealWithoutNotification, emptyList())

        val meals = fakeRepository.getMeals(dietId)
        assertEquals(2, meals.size)

        val lunch = meals.find { it.meal.type == MealType.LUNCH }?.meal
        val dinner = meals.find { it.meal.type == MealType.DINNER }?.meal

        assertTrue(lunch!!.isNotificationEnabled)
        assertFalse(dinner!!.isNotificationEnabled)
    }
}
