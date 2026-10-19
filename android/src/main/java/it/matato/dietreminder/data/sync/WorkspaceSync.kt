package it.matato.dietreminder.data.sync

import it.matato.dietreminder.client.UserService
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import it.matato.dietreminder.common.model.ApiErrorDto
import retrofit2.HttpException

/** Pure orchestration: all durable progress lives in the Room journal. */
class WorkspaceSync(private val journal: SyncJournal, private val api: UserService, private val stillCurrent: () -> Boolean = { true }) {
    suspend fun run(): Boolean {
        repeat(5) {
            if (!stillCurrent()) return true
            val current = journal.database.withTransaction { journal.state() }
            if (current.conflictVersion != null) return true
            val pending = journal.prepare()
            try {
                if (pending != null) {
                    val ack = api.putWorkspace(pending.request)
                    if (!stillCurrent()) return true
                    journal.acknowledge(ack)
                } else {
                    val remote = api.getWorkspace()
                    if (!stillCurrent()) return true
                    journal.applyRemote(remote)
                    val latest = journal.database.withTransaction { journal.state() }
                    if (!latest.dirty || latest.conflictVersion != null) return true
                }
            } catch (ex: CancellationException) { throw ex }
            catch (ex: HttpException) {
                val code = runCatching { Json.decodeFromString<ApiErrorDto>(ex.response()!!.errorBody()!!.string()).code }.getOrDefault("SERVER_ERROR")
                if (ex.code() == 409) {
                    val remote = api.getWorkspace()
                    if (stillCurrent()) journal.conflict(remote.version)
                    return true
                }
                if (stillCurrent()) journal.error(code, discardPending = ex.code() in listOf(400, 413, 422))
                return ex.code() in listOf(400, 401, 403, 413, 422)
            } catch (ex: java.io.IOException) {
                if (stillCurrent()) journal.error("NETWORK_ERROR")
                return false
            }
        }
        return false // More edits arrived while uploading. WorkManager retries the durable remainder.
    }
}
