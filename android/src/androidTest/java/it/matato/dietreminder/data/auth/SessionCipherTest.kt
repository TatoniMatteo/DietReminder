package it.matato.dietreminder.data.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionCipherTest {
    @Test
    fun keystoreEncryptionRoundTripsAndUsesDistinctNonces() {
        val first = KeystoreSessionCipher()
        val payload = "private-session-payload"
        val encrypted = first.encrypt(payload)
        assertNotEquals(payload, encrypted)
        assertNotEquals(encrypted, first.encrypt(payload))
        assertEquals(payload, KeystoreSessionCipher().decrypt(encrypted))
        assertTrue(runCatching { first.decrypt("invalid-ciphertext") }.isFailure)
    }
}
