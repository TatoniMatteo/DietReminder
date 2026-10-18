package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val userId: String,
    val username: String,
    val dailyCalorieGoal: Int = 2000,
    val updatedAt: Long = 0
)
