package it.matato.dietreminder.data.database

import androidx.room.TypeConverter
import it.matato.dietreminder.data.model.MealType
import java.time.DayOfWeek

class Converters {

    @TypeConverter
    fun mealTypeToString(value: MealType): String = value.name

    @TypeConverter
    fun stringToMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun dayOfWeekToInt(value: DayOfWeek): Int = value.value

    @TypeConverter
    fun intToDayOfWeek(value: Int): DayOfWeek = DayOfWeek.of(value)

    @TypeConverter
    fun stringListToString(value: List<String>): String = value.joinToString("\u001F")

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("\u001F")
}