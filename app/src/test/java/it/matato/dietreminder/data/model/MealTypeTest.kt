package it.matato.dietreminder.data.model

import it.matato.dietreminder.R
import org.junit.Assert.assertEquals
import org.junit.Test

class MealTypeTest {

    @Test
    fun mealType_resIdMappingsAreCorrect() {
        assertEquals(R.string.meal_breakfast, MealType.BREAKFAST.resId)
        assertEquals(R.string.meal_morning_snack, MealType.MORNING_SNACK.resId)
        assertEquals(R.string.meal_lunch, MealType.LUNCH.resId)
        assertEquals(R.string.meal_afternoon_snack, MealType.AFTERNOON_SNACK.resId)
        assertEquals(R.string.meal_dinner, MealType.DINNER.resId)
        assertEquals(R.string.meal_other, MealType.OTHER.resId)
    }
}
