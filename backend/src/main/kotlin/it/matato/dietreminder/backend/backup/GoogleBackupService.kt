package it.matato.dietreminder.backend.backup

import it.matato.dietreminder.backend.auth.AccountRepository
import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.backend.google.*
import it.matato.dietreminder.backend.profile.UserProfileService
import it.matato.dietreminder.backend.workspace.WorkspaceService
import it.matato.dietreminder.common.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest
import java.util.Base64
import java.time.Instant

@Serializable
data class GoogleBackupEnvelope(val formatVersion: Int = 1, val accountId: String, val exportedAt: String,
    val snapshot: WorkspaceSnapshot, val profile: UserProfileDto)

@Service
class GoogleBackupService(private val connections: GoogleDriveConnectionRepository, private val cipher: GoogleTokenCipher,
    private val gateway: GoogleDriveGateway, private val challenges: GoogleChallenges, private val verifier: GoogleTokenVerifier,
    private val accounts: AccountRepository, private val workspaces: WorkspaceService, private val profiles: UserProfileService,
    private val commits: it.matato.dietreminder.backend.workspace.WorkspaceCommitRepository,
    private val transaction: TransactionTemplate,
    @Value("\${google.client-id:}") private val clientId: String, @Value("\${google.client-secret:}") private val clientSecret: String,
    @Value("\${google.drive-redirect-uri:}") private val redirectUri: String) {
    private val json = Json { encodeDefaults = true }
    val available get() = clientId.isNotBlank() && clientSecret.isNotBlank() && redirectUri.isNotBlank() && cipher.available
    fun status(accountId: String): GoogleDriveStatus {
        val c = connections.findById(accountId).orElse(null)
        return GoogleDriveStatus(available, c != null, c?.email)
    }
    fun start(accountId: String): GoogleAuthorizationDto {
        configured()
        val proof = googleRandomToken()
        val state = challenges.issue("DRIVE", accountId, proof)
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(proof.toByteArray()))
        return GoogleAuthorizationDto("https://accounts.google.com/o/oauth2/v2/auth?" + googleQuery(mapOf("client_id" to clientId, "redirect_uri" to redirectUri,
            "response_type" to "code", "scope" to "openid email $DRIVE_SCOPE", "access_type" to "offline", "prompt" to "consent select_account",
            "state" to state, "nonce" to state, "code_challenge" to challenge, "code_challenge_method" to "S256")))
    }
    fun connect(code: String, state: String) {
        configured()
        if (code.length > 8192 || state.length > 128) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_GOOGLE_STATE", "Invalid Google response")
        val challenge = challenges.consume(state, "DRIVE")
        val account = accounts.findById(challenge.accountId!!).orElse(null)
        if (account == null || !account.enabled) throw ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in again")
        val tokens = gateway.exchange(code, challenge.verifier!!)
        val identity = verifier.verify(tokens.idToken ?: throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_GOOGLE_TOKEN", "Google identity is missing"))
        if (identity.nonce != state || (identity.email != account.email && identity.subject != account.googleSubject))
            throw ApiException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_MISMATCH", "Choose the Google account associated with your Diet Reminder account")
        if (DRIVE_SCOPE !in tokens.scope.split(' ')) throw ApiException(HttpStatus.BAD_REQUEST, "DRIVE_PERMISSION_REQUIRED", "Allow access to the private backup folder")
        val refresh = tokens.refreshToken ?: throw ApiException(HttpStatus.BAD_REQUEST, "DRIVE_RECONNECT_REQUIRED", "Authorize offline access to Google Drive")
        connections.save(GoogleDriveConnection(account.id, cipher.encrypt(account.id, refresh), identity.email))
    }
    fun disconnect(accountId: String) {
        val c = connections.findById(accountId).orElse(null) ?: return
        val refresh = cipher.decrypt(accountId, c.encryptedRefreshToken)
        connections.deleteById(accountId)
        runCatching { gateway.revoke(refresh) }
    }
    fun list(accountId: String) = gateway.list(access(accountId), accountId)
    fun create(accountId: String): GoogleBackupDto {
        val access = access(accountId)
        if (gateway.list(access, accountId).size >= 50) throw ApiException(HttpStatus.CONFLICT, "BACKUP_LIMIT_REACHED", "Delete an older backup before creating another (maximum 50)")
        val envelope = transaction.execute { GoogleBackupEnvelope(accountId = accountId, exportedAt = Instant.now().toString(), snapshot = workspaces.get(accountId).snapshot, profile = profiles.get(accountId)) }!!
        val payload = json.encodeToString(GoogleBackupEnvelope.serializer(), envelope)
        if (payload.toByteArray().size > 9 * 1024 * 1024) throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "BACKUP_TOO_LARGE", "Backup exceeds the size limit")
        return gateway.upload(access, accountId, "DietReminder-${envelope.exportedAt.replace(':', '-')}.json", payload)
    }
    fun restore(accountId: String, request: GoogleRestoreRequest): WorkspaceAck {
        if (request.baseVersion < 0 || !request.requestId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid restore version or request ID")
        val payload = gateway.download(access(accountId), accountId, request.fileId)
        val backup = try { json.decodeFromString<GoogleBackupEnvelope>(payload).also {
            require(it.formatVersion == 1 && it.accountId == accountId && it.profile.userId == accountId)
            it.snapshot.requireValid()
        } } catch (_: IllegalArgumentException) { throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_BACKUP", "Backup format or owner is invalid") }
        return transaction.execute {
            accounts.lockById(accountId) ?: throw ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Account not found")
            val internalId = "restore-" + googleHash(request.requestId + ":" + payload).take(48)
            val replay = commits.findByAccountIdAndRequestId(accountId, internalId) != null
            val result = workspaces.put(accountId, WorkspacePutRequest(request.baseVersion, internalId, backup.snapshot.copy(schemaVersion = 2)))
            if (!replay) profiles.save(backup.profile)
            result.copy(requestId = request.requestId)
        }!!
    }
    fun delete(accountId: String, fileId: String) = gateway.delete(access(accountId), accountId, fileId)
    private fun access(accountId: String): String {
        configured()
        val c = connections.findById(accountId).orElse(null) ?: throw ApiException(HttpStatus.CONFLICT, "DRIVE_NOT_CONNECTED", "Connect Google Drive first")
        return gateway.refresh(cipher.decrypt(accountId, c.encryptedRefreshToken))
    }
    private fun configured() { if (!available) throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_NOT_CONFIGURED", "Google Drive is not configured") }
}
