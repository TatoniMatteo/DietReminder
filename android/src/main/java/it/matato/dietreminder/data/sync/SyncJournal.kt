package it.matato.dietreminder.data.sync

import androidx.room.withTransaction
import it.matato.dietreminder.common.model.*
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.database.entity.AppConfig
import it.matato.dietreminder.data.repository.delegating.OfflineWriteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class PendingWorkspace(val generation: Long, val request: WorkspacePutRequest)
@Serializable
data class SyncState(
    val baseVersion: Long = 0,
    val generation: Long = 0,
    val acknowledgedGeneration: Long = 0,
    val pending: PendingWorkspace? = null,
    val conflictVersion: Long? = null,
    val lastSyncedAt: Long = 0,
    val error: String? = null
) {
    val dirty: Boolean get() = generation > acknowledgedGeneration
}

class SyncJournal(
    val database: AppDatabase,
    private val onDirty: () -> Unit = {},
    private val canWrite: () -> Boolean = { true }
) {
    private val json = Json { encodeDefaults = true }
    private val cache = WorkspaceCache(database)
    val states = database.configDao().observeConfig(KEY).map { row ->
        runCatching { row?.value?.let { json.decodeFromString<SyncState>(it) } ?: SyncState() }
            .getOrElse { SyncState(error = "INVALID_LOCAL_STATE") }
    }.flowOn(Dispatchers.IO)
    suspend fun state(): SyncState {
        database.configDao().getConfig(KEY)?.let { return json.decodeFromString(it.value) }
        val existing = cache.read()
        val initial = SyncState(generation = if (existing.diets.isNotEmpty() || existing.lists.isNotEmpty()) 1 else 0)
        save(initial)
        return initial
    }
    private suspend fun save(state: SyncState) = database.configDao().insertConfig(AppConfig(KEY, json.encodeToString(state)))

    suspend fun <T> mutate(action: suspend () -> T): T {
        if (!canWrite()) throw OfflineWriteException()
        val result = database.withTransaction {
            val before = state()
            val value = action()
            // Read after nested mutations, but retain state across a developer reset that clears app_config.
            val after = database.configDao().getConfig(KEY)?.let { json.decodeFromString<SyncState>(it.value) } ?: before
            save(after.copy(generation = after.generation + 1, error = null))
            value
        }
        runCatching { onDirty() }
        return result
    }
    suspend fun prepare(): PendingWorkspace? = database.withTransaction {
        val state = state()
        if (state.conflictVersion != null) return@withTransaction null
        state.pending?.let { return@withTransaction it }
        if (!state.dirty) return@withTransaction null
        val snapshot = cache.read().also { it.requireValid() }
        val pending = PendingWorkspace(state.generation, WorkspacePutRequest(state.baseVersion, UUID.randomUUID().toString(), snapshot))
        save(state.copy(pending = pending, error = null))
        pending
    }
    suspend fun acknowledge(ack: WorkspaceAck) = database.withTransaction {
        val state = state()
        val pending = state.pending ?: return@withTransaction
        check(pending.request.requestId == ack.requestId && ack.version > pending.request.baseVersion)
        save(state.copy(baseVersion = ack.version, acknowledgedGeneration = pending.generation, pending = null, error = null, lastSyncedAt = System.currentTimeMillis()))
    }
    suspend fun applyRemote(remote: WorkspaceDto, force: Boolean = false, expectedGeneration: Long? = null): Boolean = database.withTransaction {
        remote.snapshot.requireValid()
        val state = state()
        if (force && state.generation != expectedGeneration) return@withTransaction false
        if (!force && (state.dirty || state.pending != null)) return@withTransaction false
        if (!force && remote.version < state.baseVersion) {
            save(state.copy(conflictVersion = remote.version, error = "WORKSPACE_CONFLICT"))
            return@withTransaction false
        }
        if (force || remote.version != state.baseVersion) cache.replace(remote.snapshot)
        save(state.copy(baseVersion = remote.version, acknowledgedGeneration = state.generation, pending = null, conflictVersion = null, error = null, lastSyncedAt = System.currentTimeMillis()))
        true
    }
    suspend fun conflict(version: Long) = database.withTransaction {
        val state = state()
        save(state.copy(pending = null, conflictVersion = version, error = "WORKSPACE_CONFLICT"))
    }
    suspend fun keepLocal(version: Long) = database.withTransaction {
        val state = state()
        save(state.copy(baseVersion = version, pending = null, conflictVersion = null, generation = state.generation + 1, error = null))
    }
    suspend fun error(code: String, discardPending: Boolean = false) = database.withTransaction {
        val state = state()
        save(state.copy(error = code, pending = if (discardPending) null else state.pending))
    }
    companion object { const val KEY = "_cloud.workspace" }
}
