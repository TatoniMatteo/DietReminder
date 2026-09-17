package it.matato.dietreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import it.matato.dietreminder.R
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "diets")
data class DietEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val nextMealWindowMinutes: Int = 90,
    val isActive: Boolean = false
)

@Serializable
@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = DietEntity::class,
            parentColumns = ["id"],
            childColumns = ["dietId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dietId")],
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dietId: Long,
    val dayOfWeek: DayOfWeek,
    val type: MealType,
    val timeMinutes: Int,
    val description: String = "",
    val customTypeLabel: String? = null
)

@Serializable
@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId")]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val name: String,
    val order: Int = 0
)

@Serializable
@Entity(
    tableName = "food_items",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val name: String,
    val quantities: List<String> = emptyList(),
    val order: Int = 0
)

@Serializable
enum class MealType(val resId: Int) {
    @SerialName("BREAKFAST")
    BREAKFAST(R.string.meal_breakfast),

    @SerialName("MORNING_SNACK")
    MORNING_SNACK(R.string.meal_morning_snack),

    @SerialName("LUNCH")
    LUNCH(R.string.meal_lunch),

    @SerialName("AFTERNOON_SNACK")
    AFTERNOON_SNACK(R.string.meal_afternoon_snack),

    @SerialName("DINNER")
    DINNER(R.string.meal_dinner),

    @SerialName("OTHER")
    OTHER(R.string.meal_other),
}

@Serializable
data class DietExport(
    val uuid: String? = null,
    val name: String,
    val nextMealWindowMinutes: Int,
    val meals: List<MealExport>
)

@Serializable
data class MealExport(
    val day: DayOfWeek,
    val type: MealType,
    val time: String,
    val description: String = "",
    val customTypeLabel: String? = null,
    val courses: List<CourseExport> = emptyList()
)

@Serializable
data class CourseExport(
    val name: String,
    val order: Int,
    val items: List<FoodItemExport>
)

@Serializable
data class FoodItemExport(
    val name: String,
    val quantities: List<String>,
    val order: Int
)

@Serializable
@Entity(tableName = "meal_default_times")
data class MealDefaultTimeEntity(
    @PrimaryKey val type: MealType,
    val timeMinutes: Int
)

@Serializable
data class HydrationRange(
    val startMinutes: Int,
    val endMinutes: Int
)

@Serializable
data class ScheduledAlarm(
    val id: Int,
    val type: String,
    val timeMillis: Long,
    val label: String
)

@Serializable
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

class Converters {
    @TypeConverter
    fun type(value: MealType) = value.name
}
