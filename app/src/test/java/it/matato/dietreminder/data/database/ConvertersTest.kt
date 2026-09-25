package it.matato.dietreminder.data.database

import it.matato.dietreminder.data.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun mealTypeToString_and_stringToMealType() {
        for (type in MealType.entries) {
            val stringValue = converters.mealTypeToString(type)
            assertEquals(type.name, stringValue)

            val convertedType = converters.stringToMealType(stringValue)
            assertEquals(type, convertedType)
        }
    }

    @Test
    fun dayOfWeekToInt_and_intToDayOfWeek() {
        for (day in DayOfWeek.entries) {
            val intValue = converters.dayOfWeekToInt(day)
            assertEquals(day.value, intValue)

            val convertedDay = converters.intToDayOfWeek(intValue)
            assertEquals(day, convertedDay)
        }
    }

    @Test
    fun stringListToString_and_stringToStringList() {
        val list = listOf("Apple", "Banana", "Cherry")
        val stringValue = converters.stringListToString(list)

        val convertedList = converters.stringToStringList(stringValue)
        assertEquals(list, convertedList)

        assertTrue(converters.stringToStringList("").isEmpty())
    }
}
