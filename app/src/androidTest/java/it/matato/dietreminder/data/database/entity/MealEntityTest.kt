package it.matato.dietreminder.data.database.entity

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.model.MealType
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealEntityTest {
    @Test
    fun testMealCreation() {
        val meal = Meal(
            id = 1L,
            dietId = 1L,
            dayOfWeek = DayOfWeek.MONDAY,
            type = MealType.LUNCH,
            timeMinutes = 13 * 60,
            description = "Pranzo di prova"
        )
        assertEquals(1L, meal.id)
        assertEquals(DayOfWeek.MONDAY, meal.dayOfWeek)
        assertEquals(MealType.LUNCH, meal.type)
        assertEquals("Pranzo di prova", meal.description)
    }
}
