package it.matato.dietreminder.data.export

import kotlinx.serialization.Serializable

@Serializable
data class CourseExport(
    val name: String,
    val order: Int,
    val items: List<FoodItemExport>
)