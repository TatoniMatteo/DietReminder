package it.matato.dietreminder.backend.backup

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.common.model.GoogleBackupDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
fun googleQuery(values: Map<String, String>): String = values.entries.joinToString("&") { URLEncoder.encode(it.key, Charsets.UTF_8) + "=" + URLEncoder.encode(it.value, Charsets.UTF_8) }
data class GoogleTokens(val accessToken: String, val refreshToken: String?, val idToken: String?, val scope: String)

interface GoogleDriveGateway {
    fun exchange(code: String, verifier: String): GoogleTokens
    fun refresh(refreshToken: String): String
    fun revoke(refreshToken: String)
    fun list(access: String, accountId: String): List<GoogleBackupDto>
    fun upload(access: String, accountId: String, name: String, payload: String): GoogleBackupDto
    fun download(access: String, accountId: String, fileId: String): String
    fun delete(access: String, accountId: String, fileId: String)
}
@Component
class HttpGoogleDriveGateway(private val mapper: ObjectMapper,
    @Value("\${google.client-id:}") private val clientId: String,
    @Value("\${google.client-secret:}") private val clientSecret: String,
    @Value("\${google.drive-redirect-uri:}") private val redirectUri: String) : GoogleDriveGateway {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()
    private fun token(values: Map<String, String>): JsonNode = json(call("https://oauth2.googleapis.com/token", "POST", googleQuery(values + mapOf("client_id" to clientId, "client_secret" to clientSecret)), "application/x-www-form-urlencoded"))
    override fun exchange(code: String, verifier: String): GoogleTokens {
        val node = token(mapOf("code" to code, "code_verifier" to verifier, "redirect_uri" to redirectUri, "grant_type" to "authorization_code"))
        return GoogleTokens(node.path("access_token").asText(), node.path("refresh_token").asText().takeIf { it.isNotBlank() }, node.path("id_token").asText().takeIf { it.isNotBlank() }, node.path("scope").asText())
    }
    override fun refresh(refreshToken: String): String = token(mapOf("refresh_token" to refreshToken, "grant_type" to "refresh_token")).path("access_token").asText().also { if (it.isBlank()) throw ApiException(HttpStatus.CONFLICT, "DRIVE_RECONNECT_REQUIRED", "Reconnect Google Drive") }
    override fun revoke(refreshToken: String) { call("https://oauth2.googleapis.com/revoke", "POST", googleQuery(mapOf("token" to refreshToken)), "application/x-www-form-urlencoded") }
    override fun list(access: String, accountId: String): List<GoogleBackupDto> {
        val q = "trashed = false and appProperties has { key='dietReminderAccountId' and value='$accountId' }"
        val result = json(call("https://www.googleapis.com/drive/v3/files?" + googleQuery(mapOf("spaces" to "appDataFolder", "q" to q, "fields" to "files(id,name,createdTime,size)", "orderBy" to "createdTime desc", "pageSize" to "100")), access = access))
        return result.path("files").map(::backup)
    }
    override fun upload(access: String, accountId: String, name: String, payload: String): GoogleBackupDto {
        val boundary = "dietreminder-" + UUID.randomUUID()
        val metadata = mapper.writeValueAsString(mapOf("name" to name, "mimeType" to "application/json", "parents" to listOf("appDataFolder"), "appProperties" to mapOf("dietReminderAccountId" to accountId)))
        val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$payload\r\n--$boundary--\r\n"
        return backup(json(call("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,createdTime,size", "POST", body, "multipart/related; boundary=$boundary", access)))
    }
    private fun ownFile(access: String, accountId: String, fileId: String) {
        if (!fileId.matches(Regex("[A-Za-z0-9_-]{1,200}"))) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid backup ID")
        val metadata = json(call("https://www.googleapis.com/drive/v3/files/$fileId?fields=appProperties,spaces,trashed", access = access))
        if (metadata.path("appProperties").path("dietReminderAccountId").asText() != accountId || metadata.path("trashed").asBoolean() || !metadata.path("spaces").any { it.asText() == "appDataFolder" })
            throw ApiException(HttpStatus.NOT_FOUND, "BACKUP_NOT_FOUND", "Backup not found for this account")
    }
    override fun download(access: String, accountId: String, fileId: String): String {
        ownFile(access, accountId, fileId)
        return call("https://www.googleapis.com/drive/v3/files/$fileId?alt=media", access = access)
    }
    override fun delete(access: String, accountId: String, fileId: String) {
        ownFile(access, accountId, fileId)
        call("https://www.googleapis.com/drive/v3/files/$fileId", "DELETE", access = access)
    }
    private fun backup(n: JsonNode) = GoogleBackupDto(n.path("id").asText(), n.path("name").asText(), n.path("createdTime").asText(), n.path("size").asText().takeIf { it.isNotBlank() })
    private fun json(text: String): JsonNode = try { mapper.readTree(text) ?: throw java.io.IOException() }
    catch (_: java.io.IOException) { throw ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_REQUEST_FAILED", "Google returned an invalid response") }
    private fun call(url: String, method: String = "GET", body: String? = null, type: String = "application/json", access: String? = null): String {
        try {
            val req = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(30)).header("Content-Type", type)
                .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
            if (access != null) req.header("Authorization", "Bearer $access")
            val response = http.send(req.build(), HttpResponse.BodyHandlers.ofInputStream())
            val bytes = response.body().use { it.readNBytes(9 * 1024 * 1024 + 1) }
            if (bytes.size > 9 * 1024 * 1024) throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "BACKUP_TOO_LARGE", "Backup exceeds the size limit")
            if (response.statusCode() !in 200..299) {
                val reconnect = response.statusCode() == 401 || runCatching { json(String(bytes)).path("error").asText() == "invalid_grant" }.getOrDefault(false)
                throw ApiException(if (reconnect) HttpStatus.CONFLICT else HttpStatus.BAD_GATEWAY, if (reconnect) "DRIVE_RECONNECT_REQUIRED" else "GOOGLE_REQUEST_FAILED", if (reconnect) "Reconnect Google Drive from settings" else "Google Drive is temporarily unavailable")
            }
            return String(bytes, Charsets.UTF_8)
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt(); throw ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_REQUEST_FAILED", "Google request interrupted")
        } catch (_: java.io.IOException) {
            throw ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_REQUEST_FAILED", "Google Drive is temporarily unavailable")
        }
    }
}
