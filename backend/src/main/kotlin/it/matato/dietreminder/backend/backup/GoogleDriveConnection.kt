package it.matato.dietreminder.backend.backup

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Entity
@Table(name = "google_drive_connections")
class GoogleDriveConnection(@Id val accountId: String, @Column(columnDefinition = "TEXT", nullable = false) var encryptedRefreshToken: String,
    @Column(length = 254, nullable = false) var email: String)
interface GoogleDriveConnectionRepository : JpaRepository<GoogleDriveConnection, String>

/** Separate encryption key, never stored alongside the database or sent to clients. */
@Component
class GoogleTokenCipher(@Value("\${google.token-encryption-key:}") key: String) {
    private val keyBytes = if (key.isBlank()) null else Base64.getDecoder().decode(key).also { require(it.size == 32) { "GOOGLE_TOKEN_ENCRYPTION_KEY must contain 32 base64-encoded bytes" } }
    val available get() = keyBytes != null
    fun encrypt(accountId: String, value: String): String {
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = cipher(Cipher.ENCRYPT_MODE, iv, accountId)
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(value.toByteArray()))
    }
    fun decrypt(accountId: String, value: String): String {
        val bytes = Base64.getDecoder().decode(value)
        return String(cipher(Cipher.DECRYPT_MODE, bytes.copyOfRange(0, 12), accountId).doFinal(bytes.copyOfRange(12, bytes.size)))
    }
    private fun cipher(mode: Int, iv: ByteArray, accountId: String): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, SecretKeySpec(requireNotNull(keyBytes) { "Google backup encryption is not configured" }, "AES"), GCMParameterSpec(128, iv))
        updateAAD(accountId.toByteArray())
    }
}
