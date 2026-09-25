package it.matato.dietreminder.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.model.MealType
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConvertersInstrumentedTest {

    private val converters = Converters()

    @Test
    fun testMealTypeConverters() {
        val mealType = MealType.LUNCH
        val converted = converters.mealTypeToString(mealType)
        assertEquals("LUNCH", converted)
        assertEquals(mealType, converters.stringToMealType(converted))
    }

    @Test
    fun testDayOfWeekConverters() {
        val day = DayOfWeek.MONDAY
        val converted = converters.dayOfWeekToInt(day)
        assertEquals(1, converted)
        assertEquals(day, converters.intToDayOfWeek(converted))
    }

    @Test
    fun testStringListConverters() {
        val list = listOf("apple", "banana", "cherry")
        val converted = converters.stringListToString(list)
        assertEquals("apple\u001Fbanana\u001Fcherry", converted)
        assertEquals(list, converters.stringToStringList(converted))

        assertEquals(emptyList<String>(), converters.stringToStringList(""))
    }
}
