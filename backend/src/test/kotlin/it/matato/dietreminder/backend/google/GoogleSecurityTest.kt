package it.matato.dietreminder.backend.google

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import it.matato.dietreminder.backend.backup.GoogleTokenCipher
import it.matato.dietreminder.backend.error.ApiException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.Date

class GoogleSecurityTest {
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val verifier = GoogleTokenVerifier(NimbusJwtDecoder.withPublicKey(keys.public as RSAPublicKey).build(), "web-client-id")
    private fun signed(audience: String = "web-client-id", issuer: String = "https://accounts.google.com", expiry: Instant = Instant.now().plusSeconds(300), verified: Boolean = true, otherKey: Boolean = false): String {
        val claims = JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("stable-subject").issueTime(Date()).expirationTime(Date.from(expiry))
            .claim("email", "person@gmail.com").claim("email_verified", verified).claim("name", "Person").claim("nonce", "nonce-bound-to-request").build()
        val token = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claims)
        val privateKey = if (otherKey) KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().private else keys.private
        token.sign(RSASSASigner(privateKey)); return token.serialize()
    }
    @Test fun `valid Google identity requires a trusted signature and expected claims`() {
        assertEquals("stable-subject", verifier.verify(signed()).subject)
        for (token in listOf(signed(audience = "another-client"), signed(issuer = "https://attacker.example"), signed(expiry = Instant.now().minusSeconds(120)), signed(verified = false), signed(otherKey = true), "not-a-jwt")) {
            assertThrows(ApiException::class.java) { verifier.verify(token) }
        }
    }
    @Test fun `refresh tokens are encrypted and cannot be moved to another account`() {
        val cipher = GoogleTokenCipher(Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }))
        val encrypted = cipher.encrypt("account-a", "secret-refresh-token")
        assertNotEquals(encrypted, cipher.encrypt("account-a", "secret-refresh-token"))
        assertFalse(encrypted.contains("secret-refresh-token"))
        assertEquals("secret-refresh-token", cipher.decrypt("account-a", encrypted))
        assertThrows(Exception::class.java) { cipher.decrypt("account-b", encrypted) }
        assertThrows(IllegalArgumentException::class.java) { GoogleTokenCipher("invalid") }
    }
}
