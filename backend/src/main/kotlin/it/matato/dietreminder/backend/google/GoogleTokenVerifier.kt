package it.matato.dietreminder.backend.google

import it.matato.dietreminder.backend.error.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Locale

data class GoogleIdentity(val subject: String, val email: String, val name: String, val nonce: String)

@Service
class GoogleTokenVerifier(private val googleJwtDecoder: JwtDecoder, @Value("\${google.client-id:}") val clientId: String) {
    fun verify(token: String): GoogleIdentity {
        if (clientId.isBlank()) throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_NOT_CONFIGURED", "Google sign-in is not configured")
        try {
            require(token.length in 1..16384)
            val jwt = googleJwtDecoder.decode(token)
            require(jwt.getClaimAsString("iss") in setOf("https://accounts.google.com", "accounts.google.com"))
            require(clientId in jwt.audience)
            require(jwt.expiresAt?.isAfter(Instant.now()) == true)
            require(jwt.claims["email_verified"] == true)
            val subject = jwt.subject.orEmpty()
            val email = jwt.getClaimAsString("email").orEmpty().trim().lowercase(Locale.ROOT)
            val nonce = jwt.getClaimAsString("nonce").orEmpty()
            require(subject.isNotBlank() && subject.length <= 255 && email.length <= 254 && email.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) && nonce.isNotBlank())
            return GoogleIdentity(subject, email, jwt.getClaimAsString("name").orEmpty().ifBlank { email.substringBefore('@') }.take(100), nonce)
        } catch (_: JwtException) {
            throw invalidToken()
        } catch (_: IllegalArgumentException) {
            throw invalidToken()
        }
    }
    private fun invalidToken() = ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google identity could not be verified")
}
