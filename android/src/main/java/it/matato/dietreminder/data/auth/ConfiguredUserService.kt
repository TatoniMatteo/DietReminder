package it.matato.dietreminder.data.auth

import it.matato.dietreminder.client.BackendClient
import it.matato.dietreminder.client.UserService
import it.matato.dietreminder.common.model.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Each call pins its destination and credentials to the same account snapshot. */
@Singleton
class ConfiguredUserService @Inject constructor(private val sessions: AccountSessionStore) : UserService {
    private suspend fun <T> request(call: suspend (UserService) -> T): T {
        val state = sessions.state.value
        check(state.serverUrl.isNotEmpty()) { "Configure a server first" }
        val token = state.session?.takeIf { it.expiresAt > System.currentTimeMillis() }?.token
        val api = BackendClient.create(state.serverUrl, tokenProvider = { token }, onUnauthorized = {
            if (sessions.state.value.serverUrl == state.serverUrl) sessions.unauthorized(it)
        })
        val result = call(api)
        if (sessions.state.value.serverUrl != state.serverUrl) throw IOException("Server changed during request")
        return result
    }

    override suspend fun enrollment(): EnrollmentDto = request { it.enrollment() }
    override suspend fun verifyEmail(request: VerifyEmailRequest): AccountDto = request { it.verifyEmail(request) }
    override suspend fun resendVerification() = request { it.resendVerification() }
    override suspend fun acknowledgeAdminIntroduction(): AccountDto = request { it.acknowledgeAdminIntroduction() }
    override suspend fun googleProviders(): GoogleProvidersDto = request { it.googleProviders() }
    override suspend fun googleChallenge(): GoogleChallengeDto = request { it.googleChallenge() }
    override suspend fun googleSignIn(request: GoogleSignInRequest): SessionDto = request { it.googleSignIn(request) }
    override suspend fun linkGoogle(request: GoogleSignInRequest): AccountDto = request { it.linkGoogle(request) }
    override suspend fun googleDriveStatus(): GoogleDriveStatus = request { it.googleDriveStatus() }
    override suspend fun authorizeGoogleDrive(): GoogleAuthorizationDto = request { it.authorizeGoogleDrive() }
    override suspend fun disconnectGoogleDrive() = request { it.disconnectGoogleDrive() }
    override suspend fun googleBackups(): List<GoogleBackupDto> = request { it.googleBackups() }
    override suspend fun createGoogleBackup(): GoogleBackupDto = request { it.createGoogleBackup() }
    override suspend fun restoreGoogleBackup(request: GoogleRestoreRequest): WorkspaceAck = request { it.restoreGoogleBackup(request) }
    override suspend fun deleteGoogleBackup(id: String) = request { it.deleteGoogleBackup(id) }
    override suspend fun getWorkspace(): WorkspaceDto = request { it.getWorkspace() }
    override suspend fun putWorkspace(request: WorkspacePutRequest): WorkspaceAck = request { it.putWorkspace(request) }
    override suspend fun register(request: RegisterRequest): SessionDto = request { it.register(request) }
    override suspend fun login(request: LoginRequest): SessionDto = request { it.login(request) }
    override suspend fun me(): AccountDto = request { it.me() }
    override suspend fun logout(authorization: String?) = request { it.logout(authorization) }
    override suspend fun forgotPassword(request: ForgotPasswordRequest) = request { it.forgotPassword(request) }
    override suspend fun resetPassword(request: ResetPasswordRequest) = request { it.resetPassword(request) }
    override suspend fun changePassword(request: ChangePasswordRequest) = request { it.changePassword(request) }
    override suspend fun getProfile(): UserProfileDto = request { it.getProfile() }
    override suspend fun saveProfile(profile: ProfileUpdateRequest): UserProfileDto = request { it.saveProfile(profile) }
    override suspend fun pushSync(record: SyncPushRequest): SyncRecordDto = request { it.pushSync(record) }
    override suspend fun pullSync(afterId: Long, limit: Int): List<SyncRecordDto> = request { it.pullSync(afterId, limit) }
}
