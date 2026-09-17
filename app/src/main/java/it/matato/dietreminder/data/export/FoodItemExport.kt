package it.matato.dietreminder.data.export

import kotlinx.serialization.Serializable

@Serializable
data class FoodItemExport(
    val name: String,
    val quantities: List<String>,
    val order: Int
)