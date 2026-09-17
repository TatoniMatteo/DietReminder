package it.matato.dietreminder.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.Meal

data class MealWithDetails(
    @Embedded
    val meal: Meal,

    @Relation(
        entity = Course::class,
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val courses: List<CourseWithItems>
)