package it.matato.dietreminder.data.export

import kotlinx.serialization.Serializable

@Serializable
data class CourseExport(
    val name: String,
    val order: Int = 0,
    val alternativeGroup: String? = null,
    val description: String = "",
    val items: List<FoodItemExport> = emptyList(),
)
