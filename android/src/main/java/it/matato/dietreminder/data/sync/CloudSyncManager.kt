package it.matato.dietreminder.data.sync

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import it.matato.dietreminder.client.BackendClient
import it.matato.dietreminder.data.auth.AccountRepositories
import it.matato.dietreminder.data.auth.AccountSessionStore
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.widget.DietReminder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class CloudSyncStatus(val running: Boolean = false, val pending: Boolean = false, val conflict: Boolean = false, val error: String? = null, val lastSyncedAt: Long = 0)

@Singleton
class CloudSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessions: AccountSessionStore,
    private val repositories: AccountRepositories,
    private val scheduler: CloudSyncScheduler
) {
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val activity = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    fun observe(scope: String): Flow<CloudSyncStatus> {
        val journal = repositories.forScope(scope).journal ?: return flowOf(CloudSyncStatus())
        return combine(journal.states, activity.getOrPut(scope) { MutableStateFlow(false) }) { state, running ->
            CloudSyncStatus(running, state.dirty || state.pending != null, state.conflictVersion != null, state.error, state.lastSyncedAt)
        }
    }
    fun request(scope: String) = scheduler.enqueue(scope)

    suspend fun sync(scope: String): Boolean = locks.getOrPut(scope) { Mutex() }.withLock {
        val state = sessions.state.value
        val session = state.session ?: return@withLock true
        if (state.scope != scope || sessions.token() == null) return@withLock true
        val journal = repositories.forScope(scope).journal ?: return@withLock true
        val running = activity.getOrPut(scope) { MutableStateFlow(false) }
        val current = { sessions.state.value.scope == scope && sessions.token() == session.token }
        // Pin credentials to this run. A switch to another account cannot redirect a pending upload.
        val api = BackendClient.create(state.serverUrl, tokenProvider = { session.token }, onUnauthorized = { if (sessions.state.value.serverUrl == state.serverUrl) sessions.unauthorized(it) })
        running.value = true
        try {
            WorkspaceSync(journal, api, current).run()
        } catch (ex: CancellationException) { throw ex }
        catch (_: java.io.IOException) {
            if (current()) journal.error("NETWORK_ERROR")
            false
        } catch (_: IllegalArgumentException) {
            if (current()) journal.error("INVALID_WORKSPACE")
            true
        } finally {
            running.value = false
            if (current()) {
                AlarmSyncHelper.syncAlarms(context)
                runCatching { it.matato.dietreminder.widget.Widgets.updateAll(context) }
            }
        }
    }
    suspend fun resolve(scope: String, useServer: Boolean) {
        locks.getOrPut(scope) { Mutex() }.withLock {
            val state = sessions.state.value
            val session = state.session ?: error("Sign in again")
            check(state.scope == scope)
            val journal = repositories.forScope(scope).journal ?: error("No cloud cache")
            val generation = journal.database.withTransaction { journal.state().generation }
            val api = BackendClient.create(state.serverUrl, tokenProvider = { session.token }, onUnauthorized = { if (sessions.state.value.serverUrl == state.serverUrl) sessions.unauthorized(it) })
            val remote = api.getWorkspace()
            check(sessions.state.value.scope == scope && sessions.token() == session.token)
            if (useServer) {
                check(journal.applyRemote(remote, force = true, expectedGeneration = generation)) { "Local data changed; review the conflict again" }
                AlarmSyncHelper.syncAlarms(context)
                it.matato.dietreminder.widget.Widgets.updateAll(context)
            } else {
                journal.keepLocal(remote.version)
            }
        }
        scheduler.enqueue(scope)
    }
}
