package it.matato.dietreminder.data.auth

import org.junit.Assert.*
import org.junit.Test

class ServerAddressTest {
    @Test fun normalizesRootAndKeepsExplicitPort() {
        assertEquals("https://example.com/", ServerAddress.normalize("  https://EXAMPLE.com  "))
        assertEquals("https://example.com:8443/", ServerAddress.normalize("https://example.com:8443"))
    }
    @Test fun rejectsUnsafeOrAmbiguousAddresses() {
        listOf("", "example.com", "http://example.com", "https://user:pass@example.com", "https://example.com/api/", "https://example.com/?key=x", "https://example.com/#fragment").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { ServerAddress.normalize(it, false) }
        }
    }
    @Test fun localHttpIsOnlyAvailableForExplicitDebugDevelopment() {
        assertEquals("http://10.0.2.2:8088/", ServerAddress.normalize("http://10.0.2.2:8088", true))
        assertThrows(IllegalArgumentException::class.java) { ServerAddress.normalize("http://10.0.2.2:8088", false) }
        assertThrows(IllegalArgumentException::class.java) { ServerAddress.normalize("http://example.com", true) }
    }
}
