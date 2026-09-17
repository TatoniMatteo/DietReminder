package it.matato.dietreminder.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduledAlarm(
    val id: Int,
    val type: String,
    val timeMillis: Long,
    val label: String
)