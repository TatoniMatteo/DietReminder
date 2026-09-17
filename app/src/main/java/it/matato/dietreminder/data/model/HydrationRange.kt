package it.matato.dietreminder.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HydrationRange(
    val startMinutes: Int,
    val endMinutes: Int
)