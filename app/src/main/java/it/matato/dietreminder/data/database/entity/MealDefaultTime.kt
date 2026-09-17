package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import it.matato.dietreminder.data.model.MealType

@Entity(tableName = "meal_default_times")
data class MealDefaultTime(
    @PrimaryKey
    val type: MealType,
    val timeMinutes: Int
)