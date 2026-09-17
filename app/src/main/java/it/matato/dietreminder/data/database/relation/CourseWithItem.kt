package it.matato.dietreminder.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import it.matato.dietreminder.data.database.entity.Course
import it.matato.dietreminder.data.database.entity.FoodItem

data class CourseWithItems(
    @Embedded
    val course: Course,

    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val items: List<FoodItem>
)