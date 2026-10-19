package it.matato.dietreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.matato.dietreminder.client.UserService
import it.matato.dietreminder.common.model.*
import it.matato.dietreminder.data.auth.AccountSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(private val sessions: AccountSessionStore, private val api: UserService, private val sync: it.matato.dietreminder.data.sync.CloudSyncManager? = null) : ViewModel() {
    private val mutableServerConfiguration = MutableStateFlow(false)
    val serverConfiguration = mutableServerConfiguration.asStateFlow()
    fun openServerConfiguration() { if (!busy.value) mutableServerConfiguration.value = true }
    fun closeServerConfiguration() { mutableServerConfiguration.value = false }
    val account = sessions.state
    fun configureServer(url: String) {
        if (busy.value) return
        sessions.configureServer(url)
        if (sessions.state.value.session == null) sessions.enterCloud()
        closeServerConfiguration()
        mutableGoogle.value = GoogleProvidersDto()
        mutableGoogleBackups.value = GoogleBackupState()
        mutableEnrollment.value = emptyList()
        clearMessage()
        loadGoogleConfiguration()
    }
    private val mutableGoogle = MutableStateFlow(GoogleProvidersDto())
    val googleProviders = mutableGoogle.asStateFlow()
    data class GoogleBackupState(val accountId: String? = null, val status: GoogleDriveStatus? = null, val backups: List<GoogleBackupDto> = emptyList())
    private val mutableGoogleBackups = MutableStateFlow(GoogleBackupState())
    val googleBackupState = mutableGoogleBackups.asStateFlow()
    fun loadGoogleConfiguration() {
        val server = account.value.serverUrl
        if (server.isEmpty()) return
        viewModelScope.launch {
            try {
                val providers = api.googleProviders()
                if (account.value.serverUrl == server) mutableGoogle.value = providers
            }
            catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { /* Other login methods remain available offline. */ }
        }
    }
    fun googleSignIn(credential: suspend (String, String) -> String, link: Boolean = false) = perform {
        val providers = api.googleProviders()
        check(providers.signInEnabled)
        val nonce = api.googleChallenge().nonce
        val token = credential(providers.clientId, nonce)
        if (link) {
            val existing = sessions.state.value.session ?: return@perform
            val linked = api.linkGoogle(GoogleSignInRequest(token))
            sessions.save(existing.copy(account = linked))
        } else sessions.save(api.googleSignIn(GoogleSignInRequest(token)))
    }
    fun refreshGoogleBackups() {
        if (sessions.token() == null) return
        val token = sessions.token()
        viewModelScope.launch {
            try {
                val status = api.googleDriveStatus()
                val backups = if (status.connected) api.googleBackups() else emptyList()
                if (sessions.token() == token) {
                    mutableGoogleBackups.value = GoogleBackupState(sessions.state.value.session?.account?.userId, status, backups)
                }
            } catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { /* Explicit actions display their errors through perform. */ }
        }
    }
    fun connectGoogleDrive(open: (String) -> Unit) = perform { open(api.authorizeGoogleDrive().url) }
    fun createGoogleBackup() = perform {
        val scope = sessions.state.value.scope
        if (sync != null && (!sync.sync(scope) || sync.observe(scope).first { !it.running }.let { it.pending || it.conflict })) {
            mutableMessage.value = "BACKUP_SYNC_REQUIRED"
            return@perform
        }
        api.createGoogleBackup()
        refreshGoogleBackups()
        mutableMessage.value = "BACKUP_CREATED"
    }
    fun restoreGoogleBackup(id: String) = perform {
        val scope = sessions.state.value.scope
        if (sync != null && (!sync.sync(scope) || sync.observe(scope).first { !it.running }.let { it.pending || it.conflict })) {
            mutableMessage.value = "BACKUP_SYNC_REQUIRED"
            return@perform
        }
        val remote = api.getWorkspace()
        api.restoreGoogleBackup(GoogleRestoreRequest(id, remote.version, java.util.UUID.randomUUID().toString()))
        sync?.sync(scope)
        mutableMessage.value = "BACKUP_RESTORED"
    }
    fun deleteGoogleBackup(id: String) = perform { api.deleteGoogleBackup(id); refreshGoogleBackups() }
    fun disconnectGoogleDrive() = perform { api.disconnectGoogleDrive(); refreshGoogleBackups() }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val syncStatus = account.flatMapLatest { value ->
        sync?.observe(value.scope) ?: flowOf(it.matato.dietreminder.data.sync.CloudSyncStatus())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), it.matato.dietreminder.data.sync.CloudSyncStatus())
    fun synchronize() { sync?.request(account.value.scope) }
    fun resolveSync(useServer: Boolean) = perform { sync?.resolve(account.value.scope, useServer) }
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    fun enterCloud() { if (!busy.value) sessions.enterCloud() }
    fun useLocal() { if (!busy.value) { sessions.useLocal(); closeServerConfiguration() } }
    private val mutableEnrollment = MutableStateFlow<List<EnrollmentEventDto>>(emptyList())
    val enrollmentHistory = mutableEnrollment.asStateFlow()
    fun refreshEnrollment() = perform {
        val current = sessions.state.value.session ?: return@perform
        val status = api.enrollment()
        if (sessions.token() == current.token) {
            mutableEnrollment.value = status.history
            if (status.account != current.account) sessions.save(current.copy(account = status.account))
        }
    }
    fun verifyEmail(code: String) = perform {
        val current = sessions.state.value.session ?: return@perform
        val account = api.verifyEmail(VerifyEmailRequest(code.trim()))
        if (sessions.token() == current.token) sessions.save(current.copy(account = account))
    }
    fun resendVerification() = perform {
        api.resendVerification()
        mutableMessage.value = "VERIFICATION_SENT"
    }
    fun acknowledgeAdminIntroduction() = perform {
        val current = sessions.state.value.session ?: return@perform
        val account = api.acknowledgeAdminIntroduction()
        if (sessions.token() == current.token) sessions.save(current.copy(account = account))
    }
    fun validateSession() {
        if (sessions.token() == null) return
        synchronize()
        viewModelScope.launch {
            try {
                val current = sessions.state.value.session ?: return@launch
                val updated = api.me()
                if (sessions.token() == current.token && updated != current.account) sessions.save(current.copy(account = updated))
            }
            catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { /* Keep the same account's cache available when offline. */ }
        }
    }
    fun clearMessage() { mutableMessage.value = null }
    fun login(email: String, password: String) = perform {
        sessions.save(api.login(LoginRequest(email, password)))
    }
    fun register(email: String, password: String, username: String) = perform {
        sessions.save(api.register(RegisterRequest(email, password, username)))
    }
    fun forgot(email: String) = perform {
        api.forgotPassword(ForgotPasswordRequest(email))
        mutableMessage.value = "RESET_SENT"
    }
    fun reset(token: String, password: String) = perform {
        api.resetPassword(ResetPasswordRequest(token.trim(), password))
        mutableMessage.value = "PASSWORD_RESET"
    }
    fun changeCurrentPassword(currentPassword: String, newPassword: String) = perform {
        api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
        sessions.state.value.session?.token?.let(sessions::unauthorized)
        mutableMessage.value = "PASSWORD_CHANGED"
    }
    fun logout() = perform {
        val token = sessions.state.value.session?.token ?: return@perform
        sessions.unauthorized(token)
        kotlinx.coroutines.withTimeoutOrNull(5_000) {
            try { api.logout("Bearer $token") }
            catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { /* Local logout also works without network access. */ }
        }
    }
    private fun perform(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        mutableMessage.value = null
        viewModelScope.launch {
            try { action() }
            catch (ex: CancellationException) { throw ex }
            catch (ex: HttpException) {
                mutableMessage.value = runCatching {
                    Json.decodeFromString<ApiErrorDto>(ex.response()!!.errorBody()!!.string()).code
                }.getOrDefault("SERVER_ERROR")
            } catch (_: java.io.IOException) { mutableMessage.value = "NETWORK_ERROR" }
            catch (_: Exception) { mutableMessage.value = "SESSION_ERROR" }
            finally { mutableBusy.value = false }
        }
    }
}
