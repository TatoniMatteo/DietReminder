package it.matato.dietreminder.domain

import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientSummaryTest {

    @Test
    fun `toIngredientSummaries groups equivalent items case-insensitively and aggregates daily quantities`() {
        val meals = listOf(
            MealWithDetails(
                meal = Meal(id = 1, dietId = 1, dayOfWeek = DayOfWeek.MONDAY, type = MealType.LUNCH, timeMinutes = 780),
                courses = listOf(
                    CourseWithItems(
                        course = Course(id = 1, mealId = 1, name = "Primo"),
                        items = listOf(
                            FoodItem(id = 1, courseId = 1, name = "Spaghetti", quantities = "100g"),
                            FoodItem(id = 2, courseId = 1, name = "petto di pollo", quantities = "150g"),
                        )
                    )
                )
            ),
            MealWithDetails(
                meal = Meal(id = 2, dietId = 1, dayOfWeek = DayOfWeek.THURSDAY, type = MealType.DINNER, timeMinutes = 1200),
                courses = listOf(
                    CourseWithItems(
                        course = Course(id = 2, mealId = 2, name = "Secondo"),
                        items = listOf(
                            FoodItem(id = 3, courseId = 2, name = "Petto Di Pollo", quantities = "200g"),
                        )
                    )
                )
            )
        )

        val summaries = meals.toIngredientSummaries()

        assertEquals(2, summaries.size)

        val polloSummary = summaries.find { it.name.equals("Petto di pollo", ignoreCase = true) }
        assertEquals(2, polloSummary?.totalOccurrences)
        assertEquals(listOf("150g"), polloSummary?.dailyQuantities?.get(DayOfWeek.MONDAY))
        assertEquals(listOf("200g"), polloSummary?.dailyQuantities?.get(DayOfWeek.THURSDAY))

        val polloOccurrences = polloSummary?.occurrences ?: emptyList()
        assertEquals(2, polloOccurrences.size)
        assertEquals(DayOfWeek.MONDAY, polloOccurrences[0].dayOfWeek)
        assertEquals(MealType.LUNCH, polloOccurrences[0].mealType)
        assertEquals("Primo", polloOccurrences[0].courseName)
        assertEquals("150g", polloOccurrences[0].quantity)

        assertEquals(DayOfWeek.THURSDAY, polloOccurrences[1].dayOfWeek)
        assertEquals(MealType.DINNER, polloOccurrences[1].mealType)
        assertEquals("Secondo", polloOccurrences[1].courseName)
        assertEquals("200g", polloOccurrences[1].quantity)

        val spaghettiSummary = summaries.find { it.name.equals("Spaghetti", ignoreCase = true) }
        assertEquals(1, spaghettiSummary?.totalOccurrences)
        assertEquals(listOf("100g"), spaghettiSummary?.dailyQuantities?.get(DayOfWeek.MONDAY))
    }
}
