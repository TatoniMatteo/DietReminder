package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class EnrollmentStatus { PENDING_EMAIL, PENDING_APPROVAL, APPROVED, REJECTED }

@Serializable
enum class AccountPermission { WORKSPACE_READ, DIETS_WRITE, SHOPPING_WRITE, PROFILE_WRITE, BACKUP_MANAGE }

@Serializable
data class VerifyEmailRequest(val token: String)

@Serializable
data class EnrollmentEventDto(val id: Long, val status: EnrollmentStatus, val message: String, val createdAt: Long)

@Serializable
data class EnrollmentDto(val account: AccountDto, val history: List<EnrollmentEventDto>)

@Serializable
data class AccountReviewRequest(
    val version: Long,
    val approved: Boolean,
    val permissions: Set<AccountPermission> = AccountPermission.entries.toSet(),
    val reason: String = ""
)
