package it.matato.dietreminder.backend.profile

import it.matato.dietreminder.backend.validation.*
import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.common.model.UserProfileDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(private val profiles: UserProfileRepository) {
    @Transactional(readOnly = true)
    fun get(userId: String): UserProfileDto {
        validateUserId(userId)
        return profiles.findByUserId(userId)?.toDto()
            ?: throw ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile not found")
    }

    @Transactional
    fun save(profile: UserProfileDto): UserProfileDto {
        validateUserId(profile.userId)
        validate(
            profile.username.isNotBlank() && profile.username.length <= 100,
            "username must contain 1 to 100 characters"
        )
        validate(profile.dailyCalorieGoal in 0..20000, "dailyCalorieGoal must be between 0 and 20000")
        val existing = profiles.findByUserId(profile.userId)
        return profiles.save(
            UserProfileEntity(
                id = existing?.id ?: 0, userId = profile.userId, username = profile.username.trim(),
                dailyCalorieGoal = profile.dailyCalorieGoal,
                updatedAt = System.currentTimeMillis()
            )
        ).toDto()
    }

    private fun UserProfileEntity.toDto() = UserProfileDto(
        userId = userId, username = username, dailyCalorieGoal = dailyCalorieGoal,
        updatedAt = updatedAt
    )
}
