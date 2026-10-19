package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

@Serializable data class GoogleProvidersDto(val clientId: String = "", val signInEnabled: Boolean = false, val driveEnabled: Boolean = false, val firstRegistrationRequired: Boolean = false)
@Serializable data class GoogleChallengeDto(val nonce: String)
@Serializable data class GoogleSignInRequest(val idToken: String)
@Serializable data class GoogleDriveStatus(val available: Boolean, val connected: Boolean, val email: String? = null)
@Serializable data class GoogleAuthorizationDto(val url: String)
@Serializable data class GoogleBackupDto(val id: String, val name: String, val createdTime: String, val size: String? = null)
@Serializable data class GoogleRestoreRequest(val fileId: String, val baseVersion: Long, val requestId: String)
