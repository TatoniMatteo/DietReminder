package it.matato.dietreminder

import it.matato.dietreminder.client.UserService
import it.matato.dietreminder.common.model.*

abstract class TestGoogleApi : UserService {
    override suspend fun enrollment(): EnrollmentDto = error("Unexpected enrollment call")
    override suspend fun verifyEmail(request: VerifyEmailRequest): AccountDto = error("Unexpected verification call")
    override suspend fun resendVerification(): Unit = error("Unexpected verification call")
    override suspend fun acknowledgeAdminIntroduction(): AccountDto = error("Unexpected introduction call")
    override suspend fun changePassword(request: ChangePasswordRequest): Unit = error("Unexpected password API call")

    override suspend fun googleProviders(): GoogleProvidersDto = error("Unexpected Google API call")
    override suspend fun googleChallenge(): GoogleChallengeDto = error("Unexpected Google API call")
    override suspend fun googleSignIn(request: GoogleSignInRequest): SessionDto = error("Unexpected Google API call")
    override suspend fun linkGoogle(request: GoogleSignInRequest): AccountDto = error("Unexpected Google API call")
    override suspend fun googleDriveStatus(): GoogleDriveStatus = error("Unexpected Google API call")
    override suspend fun authorizeGoogleDrive(): GoogleAuthorizationDto = error("Unexpected Google API call")
    override suspend fun disconnectGoogleDrive() = error("Unexpected Google API call")
    override suspend fun googleBackups(): List<GoogleBackupDto> = error("Unexpected Google API call")
    override suspend fun createGoogleBackup(): GoogleBackupDto = error("Unexpected Google API call")
    override suspend fun restoreGoogleBackup(request: GoogleRestoreRequest): WorkspaceAck = error("Unexpected Google API call")
    override suspend fun deleteGoogleBackup(id: String) = error("Unexpected Google API call")


}
