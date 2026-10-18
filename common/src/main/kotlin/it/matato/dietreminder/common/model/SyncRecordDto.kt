package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncRecordDto(
    val id: Long = 0,
    val userId: String,
    val dataType: String,
    val payloadJson: String,
    val timestamp: Long = 0,
    val requestId: String? = null
)
