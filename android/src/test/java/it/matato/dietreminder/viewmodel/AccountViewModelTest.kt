package it.matato.dietreminder.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.client.UserService
import it.matato.dietreminder.common.model.*
import it.matato.dietreminder.data.auth.AccountSessionStore
import it.matato.dietreminder.data.auth.TestSessionCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestDietApplication::class)
class AccountViewModelTest {
    private lateinit var sessions: AccountSessionStore
    private lateinit var api: TestAuthApi
    private lateinit var vm: AccountViewModel
    @Before
    fun prepare() {
        Dispatchers.setMain(StandardTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("auth_session", Context.MODE_PRIVATE).edit().clear().commit()
        sessions = AccountSessionStore(context, TestSessionCipher())
        sessions.configureServer("https://test.example.com/")
        api = TestAuthApi()
        vm = AccountViewModel(sessions, api)
    }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test
    fun loginPersistsSessionAndUnlocksOnlyItsAccount() = runTest {
        sessions.enterCloud()
        vm.login("alice@example.com", "long test password")
        advanceUntilIdle()
        assertEquals(api.session, sessions.state.value.session)
        assertFalse(vm.busy.value)
        assertNull(vm.message.value)
    }
    @Test
    fun invalidCredentialsLeaveCloudLockedAndExposeAUsableError() = runTest {
        sessions.enterCloud()
        api.loginFailure = HttpException(Response.error<Any>(401, """{"code":"INVALID_CREDENTIALS","message":"Invalid"}""".toResponseBody()))
        vm.login("alice@example.com", "wrong")
        advanceUntilIdle()
        assertNull(sessions.state.value.session)
        assertTrue(sessions.state.value.cloud)
        assertEquals("INVALID_CREDENTIALS", vm.message.value)
        assertFalse(vm.busy.value)
    }
    @Test
    fun logoutLocksCacheEvenWhenServerIsUnreachableAndUsesTheOriginalToken() = runTest {
        sessions.save(api.session)
        vm.logout()
        advanceUntilIdle()
        assertNull(sessions.state.value.session)
        assertTrue(sessions.state.value.cloud)
        assertEquals("Bearer ${api.session.token}", api.logoutHeader)
        assertFalse(vm.busy.value)
    }
    @Test
    fun registrationAndPasswordRecoveryCallTheSharedApi() = runTest {
        vm.register("alice@example.com", "long test password", "Alice")
        advanceUntilIdle()
        assertEquals(api.session, sessions.state.value.session)
        vm.forgot("alice@example.com")
        advanceUntilIdle()
        assertEquals("RESET_SENT", vm.message.value)
        vm.reset(" code ", "a new long password")
        advanceUntilIdle()
        assertEquals("code", api.resetToken)
        assertEquals("PASSWORD_RESET", vm.message.value)
    }
    @Test fun passwordChangePassesCurrentPasswordThenLocksTheOldSession() = runTest {
        sessions.save(api.session)
        vm.changeCurrentPassword("Old password!", "Una password nuova!")
        advanceUntilIdle()
        assertEquals(ChangePasswordRequest("Old password!", "Una password nuova!"), api.changeRequest)
        assertNull(sessions.state.value.session)
        assertEquals("PASSWORD_CHANGED", vm.message.value)
        assertFalse(vm.busy.value)
    }
    @Test fun wrongCurrentPasswordRetainsTheSessionAndShowsTheSpecificError() = runTest {
        sessions.save(api.session)
        api.changeFailure = HttpException(Response.error<Any>(400, """{"code":"INVALID_CURRENT_PASSWORD","message":"Incorrect"}""".toResponseBody()))
        vm.changeCurrentPassword("Wrong password!", "Una password nuova!")
        advanceUntilIdle()
        assertEquals(api.session,sessions.state.value.session)
        assertEquals("INVALID_CURRENT_PASSWORD",vm.message.value)
        assertFalse(vm.busy.value)
    }

    @Test fun pendingEnrollmentKeepsCloudDataLockedUntilAdministratorApproval() = runTest {
        api.session = api.session.copy(account = api.session.account.copy(emailVerified = false, enrollmentStatus = EnrollmentStatus.PENDING_EMAIL))
        vm.register("alice@example.com", "long test password", "Alice")
        advanceUntilIdle()
        assertEquals("locked", sessions.state.value.scope)
        vm.verifyEmail("code")
        advanceUntilIdle()
        assertEquals(EnrollmentStatus.PENDING_APPROVAL, sessions.state.value.session!!.account.enrollmentStatus)
        assertEquals("locked", sessions.state.value.scope)
        api.session = api.session.copy(account = api.session.account.copy(enrollmentStatus = EnrollmentStatus.APPROVED))
        vm.refreshEnrollment()
        advanceUntilIdle()
        assertTrue(sessions.state.value.session!!.account.ready)
        assertNotEquals("locked", sessions.state.value.scope)
    }
    @Test fun administratorIntroductionIsAcknowledgedWithoutReplacingItsSession() = runTest {
        api.session = api.session.copy(account = api.session.account.copy(role = "ADMIN", adminIntroductionPending = true))
        sessions.save(api.session)
        vm.acknowledgeAdminIntroduction()
        advanceUntilIdle()
        assertFalse(sessions.state.value.session!!.account.adminIntroductionPending)
        assertEquals(api.session.token, sessions.state.value.session!!.token)
    }

}

private class TestAuthApi : it.matato.dietreminder.TestGoogleApi() {
    override suspend fun getWorkspace(): WorkspaceDto = error("Unused")
    override suspend fun putWorkspace(request: WorkspacePutRequest): WorkspaceAck = error("Unused")
    var session = SessionDto("test-session", System.currentTimeMillis() + 120_000, AccountDto("alice", "alice@example.com", "Alice"))
    override suspend fun enrollment() = EnrollmentDto(session.account, emptyList())
    override suspend fun verifyEmail(request: VerifyEmailRequest): AccountDto {
        session = session.copy(account = session.account.copy(emailVerified = true, enrollmentStatus = if (session.account.role == "ADMIN") EnrollmentStatus.APPROVED else EnrollmentStatus.PENDING_APPROVAL))
        return session.account
    }
    override suspend fun resendVerification() {}
    override suspend fun acknowledgeAdminIntroduction(): AccountDto {
        session = session.copy(account = session.account.copy(adminIntroductionPending = false))
        return session.account
    }
    var loginFailure: Exception? = null
    var logoutHeader: String? = null
    var resetToken: String? = null
    var changeRequest: ChangePasswordRequest? = null
    var changeFailure: Exception? = null
    override suspend fun changePassword(request: ChangePasswordRequest) { changeFailure?.let { throw it }; changeRequest = request }
    override suspend fun login(request: LoginRequest): SessionDto { loginFailure?.let { throw it }; return session }
    override suspend fun register(request: RegisterRequest) = session
    override suspend fun logout(authorization: String?) { logoutHeader = authorization; throw java.io.IOException("Offline") }
    override suspend fun me() = session.account
    override suspend fun forgotPassword(request: ForgotPasswordRequest) {}
    override suspend fun resetPassword(request: ResetPasswordRequest) { resetToken = request.token }
    override suspend fun getProfile(): UserProfileDto = error("Unused")
    override suspend fun saveProfile(profile: ProfileUpdateRequest): UserProfileDto = error("Unused")
    override suspend fun pushSync(record: SyncPushRequest): SyncRecordDto = error("Unused")
    override suspend fun pullSync(afterId: Long, limit: Int): List<SyncRecordDto> = error("Unused")
}
