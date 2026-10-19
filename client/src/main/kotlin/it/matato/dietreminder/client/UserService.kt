package it.matato.dietreminder.client

import it.matato.dietreminder.common.model.*
import retrofit2.http.*

interface UserService {
    @GET("api/auth/enrollment") suspend fun enrollment(): EnrollmentDto
    @POST("api/auth/verify-email") suspend fun verifyEmail(@Body request: VerifyEmailRequest): AccountDto
    @POST("api/auth/resend-verification") suspend fun resendVerification()
    @POST("api/auth/admin-introduction") suspend fun acknowledgeAdminIntroduction(): AccountDto
    @GET("api/auth/providers") suspend fun googleProviders(): GoogleProvidersDto
    @POST("api/auth/google/challenge") suspend fun googleChallenge(): GoogleChallengeDto
    @POST("api/auth/google") suspend fun googleSignIn(@Body request: GoogleSignInRequest): SessionDto
    @POST("api/auth/google/link") suspend fun linkGoogle(@Body request: GoogleSignInRequest): AccountDto
    @GET("api/backups/google/status") suspend fun googleDriveStatus(): GoogleDriveStatus
    @POST("api/backups/google/authorize") suspend fun authorizeGoogleDrive(): GoogleAuthorizationDto
    @DELETE("api/backups/google/connection") suspend fun disconnectGoogleDrive()
    @GET("api/backups/google") suspend fun googleBackups(): List<GoogleBackupDto>
    @POST("api/backups/google") suspend fun createGoogleBackup(): GoogleBackupDto
    @POST("api/backups/google/restore") suspend fun restoreGoogleBackup(@Body request: GoogleRestoreRequest): WorkspaceAck
    @DELETE("api/backups/google/{id}") suspend fun deleteGoogleBackup(@Path("id") id: String)

    @GET("api/workspace") suspend fun getWorkspace(): WorkspaceDto
    @PUT("api/workspace") suspend fun putWorkspace(@Body request: WorkspacePutRequest): WorkspaceAck
    @POST("api/auth/register") suspend fun register(@Body request: RegisterRequest): SessionDto
    @POST("api/auth/login") suspend fun login(@Body request: LoginRequest): SessionDto
    @GET("api/auth/me") suspend fun me(): AccountDto
    @POST("api/auth/logout") suspend fun logout(@Header("Authorization") authorization: String? = null)
    @POST("api/auth/forgot-password") suspend fun forgotPassword(@Body request: ForgotPasswordRequest)
    @POST("api/auth/reset-password") suspend fun resetPassword(@Body request: ResetPasswordRequest)
    @POST("api/auth/change-password") suspend fun changePassword(@Body request: ChangePasswordRequest)
    @GET("api/user/profile") suspend fun getProfile(): UserProfileDto
    @POST("api/user/profile") suspend fun saveProfile(@Body profile: ProfileUpdateRequest): UserProfileDto
    @POST("api/user/sync/push") suspend fun pushSync(@Body record: SyncPushRequest): SyncRecordDto
    @GET("api/user/sync/pull")
    suspend fun pullSync(@Query("afterId") afterId: Long = 0, @Query("limit") limit: Int = 100): List<SyncRecordDto>
}
