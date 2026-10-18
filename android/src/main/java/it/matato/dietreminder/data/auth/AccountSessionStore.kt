package it.matato.dietreminder.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import it.matato.dietreminder.BuildConfig
import it.matato.dietreminder.common.model.SessionDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class AccountState(val cloud: Boolean = false, val session: SessionDto? = null) {
    val scope: String get() = if (!cloud) "local" else session?.takeIf { it.account.ready }?.let {
        "${BuildConfig.BACKEND_BASE_URL}:${it.account.userId}"
    } ?: "locked"
}

@Singleton
class AccountSessionStore @Inject constructor(@ApplicationContext context: Context, private val cipher: SessionCipher) {
    private val prefs = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutable = MutableStateFlow(restore())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            state.collectLatest { value ->
                val session = value.session
                if (session != null) {
                    delay((session.expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
                    unauthorized(session.token)
                }
            }
        }
    }
    private fun restore(): AccountState {
        if (!prefs.getBoolean("cloud", false)) return AccountState()
        val session = runCatching {
            json.decodeFromString<SessionDto>(cipher.decrypt(prefs.getString("session", null) ?: error("No session")))
        }.getOrNull()?.takeIf { it.expiresAt > System.currentTimeMillis() }
        if (session == null) prefs.edit().remove("session").commit()
        return AccountState(cloud = true, session = session)
    }
    @Synchronized
    fun save(session: SessionDto) {
        require(session.expiresAt > System.currentTimeMillis())
        val encrypted = cipher.encrypt(json.encodeToString(session))
        check(prefs.edit().putBoolean("cloud", true).putString("session", encrypted).commit())
        mutable.value = AccountState(true, session)
    }
    @Synchronized
    fun enterCloud() {
        check(prefs.edit().putBoolean("cloud", true).remove("session").commit())
        mutable.value = AccountState(true)
    }
    @Synchronized
    fun useLocal() {
        check(prefs.edit().putBoolean("cloud", false).remove("session").commit())
        mutable.value = AccountState()
    }
    @Synchronized
    fun unauthorized(token: String) {
        if (mutable.value.session?.token == token) enterCloud()
    }
    fun token(): String? {
        val session = state.value.session ?: return null
        if (session.expiresAt <= System.currentTimeMillis()) { unauthorized(session.token); return null }
        return session.token
    }
    companion object {
        fun databaseName(accountScope: String): String {
            if (accountScope == "local") return "diet.db"
            val hash = MessageDigest.getInstance("SHA-256").digest(accountScope.toByteArray()).joinToString("") { "%02x".format(it) }
            return "account_$hash.db"
        }
    }
}
