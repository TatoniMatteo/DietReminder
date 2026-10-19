package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String, val username: String)
@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class AccountDto(val userId: String, val email: String, val username: String, val role: String = "USER", val googleLinked: Boolean = false,
    val emailVerified: Boolean = true,
    val enrollmentStatus: EnrollmentStatus = EnrollmentStatus.APPROVED,
    val permissions: Set<AccountPermission> = AccountPermission.entries.toSet(),
    val reviewReason: String? = null,
    val version: Long = 0,
    val adminIntroductionPending: Boolean = false
) {
    val ready: Boolean get() = emailVerified && enrollmentStatus == EnrollmentStatus.APPROVED
    fun permits(permission: AccountPermission): Boolean = ready && (role == "ADMIN" || permission in permissions)
}
@Serializable
data class SessionDto(val token: String, val expiresAt: Long, val account: AccountDto)
@Serializable
data class ForgotPasswordRequest(val email: String)
@Serializable
data class ResetPasswordRequest(val token: String, val password: String)
@Serializable
data class ProfileUpdateRequest(val username: String, val dailyCalorieGoal: Int? = null)
@Serializable
data class SyncPushRequest(val dataType: String, val payloadJson: String, val requestId: String? = null)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
