package it.matato.dietreminder.backend.workspace

import it.matato.dietreminder.backend.auth.AccountRepository
import it.matato.dietreminder.backend.auth.toAccountDto
import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.common.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.UUID

@Service
class WorkspaceService(private val accounts: AccountRepository, private val workspaces: WorkspaceRepository, private val commits: WorkspaceCommitRepository) {
    private val json = Json { encodeDefaults = true }
    @Transactional(readOnly = true)
    fun get(accountId: String): WorkspaceDto {
        val row = workspaces.findById(accountId).orElse(null) ?: return WorkspaceDto(0, WorkspaceSnapshot())
        return WorkspaceDto(row.revision, json.decodeFromString<WorkspaceSnapshot>(row.payload))
    }
    @Transactional
    fun put(accountId: String, request: WorkspacePutRequest): WorkspaceAck {
        try {
            require(request.baseVersion >= 0 && request.requestId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) { "Invalid version or request ID" }
            request.snapshot.requireValid()
        } catch (ex: IllegalArgumentException) { throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_WORKSPACE", ex.message ?: "Invalid workspace") }
        val payload = json.encodeToString(request.snapshot)
        if (payload.toByteArray().size > 8 * 1024 * 1024) throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "WORKSPACE_TOO_LARGE", "Workspace exceeds 8 MiB")
        // Lock the existing account row, including for the very first upload; no initialization race.
        val account = accounts.lockById(accountId) ?: throw ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Account not found")
        val contentHash = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) }
        commits.findByAccountIdAndRequestId(accountId, request.requestId)?.let {
            if (it.contentHash != contentHash || request.baseVersion != it.revision - 1) throw ApiException(HttpStatus.CONFLICT, "REQUEST_ID_CONFLICT", "Request ID was already used for different data")
            return WorkspaceAck(it.revision, request.requestId)
        }
        val row = workspaces.findById(accountId).orElse(null)
        if ((row?.revision ?: 0) != request.baseVersion) throw ApiException(HttpStatus.CONFLICT, "WORKSPACE_CONFLICT", "Data changed on another device")
        if (row != null && json.decodeFromString<WorkspaceSnapshot>(row.payload).schemaVersion > request.snapshot.schemaVersion)
            throw ApiException(HttpStatus.CONFLICT, "WORKSPACE_CLIENT_OUTDATED", "Update the application before editing this workspace")
        val previous = row?.let { json.decodeFromString<WorkspaceSnapshot>(it.payload) } ?: WorkspaceSnapshot()
        val access = account.toAccountDto()
        val next = request.snapshot
        val dietsChanged = previous.diets != next.diets || previous.meals != next.meals || previous.courses != next.courses || previous.foods != next.foods
        val shoppingChanged = previous.lists != next.lists || previous.items != next.items || previous.days != next.days
        if (!access.permits(AccountPermission.WORKSPACE_READ) ||
            (dietsChanged && !access.permits(AccountPermission.DIETS_WRITE)) ||
            (shoppingChanged && !access.permits(AccountPermission.SHOPPING_WRITE)))
            throw ApiException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Your assigned permissions do not allow these changes")
        val revision = request.baseVersion + 1
        workspaces.save(WorkspaceEntity(accountId, revision, payload))
        commits.save(WorkspaceCommitEntity(UUID.randomUUID().toString(), accountId, request.requestId, revision, contentHash, payload))
        return WorkspaceAck(revision, request.requestId)
    }
}
