package it.matato.dietreminder.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.matato.dietreminder.data.model.MealType
import java.time.DayOfWeek

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = Diet::class,
            parentColumns = ["id"],
            childColumns = ["dietId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dietId")]
)
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dietId: Long,
    val dayOfWeek: DayOfWeek,
    val type: MealType,
    val timeMinutes: Int,
    val description: String = "",
    val customTypeLabel: String? = null
)