package it.matato.dietreminder.backend.google

import it.matato.dietreminder.backend.error.ApiException
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Entity
@Table(name = "google_challenges")
class GoogleChallengeEntity(
    @Id @Column(length = 64) val hash: String,
    @Column(length = 16, nullable = false) val purpose: String,
    val accountId: String? = null,
    @Column(length = 128) val verifier: String? = null,
    @Column(nullable = false) val expiresAt: Long
)
interface GoogleChallengeRepository : JpaRepository<GoogleChallengeEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByHashAndPurpose(hash: String, purpose: String): GoogleChallengeEntity?
    fun deleteByExpiresAtLessThan(expiresAt: Long)
}
fun googleRandomToken(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))
fun googleHash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

@Service
class GoogleChallenges(private val repository: GoogleChallengeRepository) {
    @Transactional
    fun issue(purpose: String, accountId: String? = null, verifier: String? = null): String {
        repository.deleteByExpiresAtLessThan(System.currentTimeMillis())
        if (repository.count() >= 10000) throw ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Try again later")
        val token = googleRandomToken()
        repository.save(GoogleChallengeEntity(googleHash(token), purpose, accountId, verifier, System.currentTimeMillis() + 5 * 60_000))
        return token
    }
    @Transactional
    fun consume(value: String, purpose: String): GoogleChallengeEntity {
        val challenge = repository.findByHashAndPurpose(googleHash(value), purpose)
        if (challenge == null || challenge.expiresAt <= System.currentTimeMillis()) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_GOOGLE_STATE", "This Google request has expired or has already been used")
        repository.delete(challenge)
        repository.flush()
        return challenge
    }
}
