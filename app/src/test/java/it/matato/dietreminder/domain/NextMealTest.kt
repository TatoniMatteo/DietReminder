package it.matato.dietreminder.domain

import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class NextMealTest {

    @Test
    fun nextMeal_returnsNull_whenMealsListIsEmpty() {
        val now = LocalDateTime.of(2025, 5, 12, 10, 0) // Monday
        val result = nextMeal(emptyList(), now, 90)
        assertNull(result)
    }

    @Test
    fun nextMeal_returnsCurrentMeal_whenNowIsInMealWindow() {
        val now = LocalDateTime.of(2025, 5, 12, 12, 30) // Monday 12:30
        val lunchMeal = MealWithDetails(
            meal = Meal(
                id = 1,
                dietId = 1,
                dayOfWeek = DayOfWeek.MONDAY,
                type = MealType.LUNCH,
                timeMinutes = 12 * 60, // 12:00
                description = "Pranzo"
            ),
            courses = emptyList()
        )

        // Window = 90 mins -> 12:00 to 13:30. 12:30 is in the window.
        val result = nextMeal(listOf(lunchMeal), now, 90)

        assertNotNull(result)
        assertEquals(1L, result?.meal?.meal?.id)
        assertEquals(LocalDateTime.of(2025, 5, 12, 12, 0), result?.dateTime)
    }

    @Test
    fun nextMeal_returnsUpcomingMeal_whenNowIsBeforeMealTime() {
        val now = LocalDateTime.of(2025, 5, 12, 11, 0) // Monday 11:00
        val lunchMeal = MealWithDetails(
            meal = Meal(
                id = 1,
                dietId = 1,
                dayOfWeek = DayOfWeek.MONDAY,
                type = MealType.LUNCH,
                timeMinutes = 13 * 60, // 13:00
                description = "Pranzo"
            ),
            courses = emptyList()
        )

        val result = nextMeal(listOf(lunchMeal), now, 90)

        assertNotNull(result)
        assertEquals(1L, result?.meal?.meal?.id)
        assertEquals(120L, result?.timeRemainingMinutes)
    }

    @Test
    fun nextMeal_returnsTomorrowMeal_whenNoMoreMealsToday() {
        val now = LocalDateTime.of(2025, 5, 12, 22, 0) // Monday 22:00
        val tuesdayBreakfast = MealWithDetails(
            meal = Meal(
                id = 2,
                dietId = 1,
                dayOfWeek = DayOfWeek.TUESDAY,
                type = MealType.BREAKFAST,
                timeMinutes = 8 * 60, // 08:00
                description = "Colazione"
            ),
            courses = emptyList()
        )

        val result = nextMeal(listOf(tuesdayBreakfast), now, 90)

        assertNotNull(result)
        assertEquals(2L, result?.meal?.meal?.id)
        assertEquals(LocalDateTime.of(2025, 5, 13, 8, 0), result?.dateTime)
    }
}
