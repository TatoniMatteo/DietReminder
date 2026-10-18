package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.backend.error.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class AuthRateLimiter(@Value("\${auth.attempt-limit:30}") private val limit: Int) {
    private data class Window(val until: Long, var count: Int)
    private val windows = mutableMapOf<String, Window>()
    @Synchronized
    fun check(address: String) {
        val now = System.currentTimeMillis()
        windows.entries.removeIf { it.value.until <= now }
        val window = windows.getOrPut(address) { Window(now + 15 * 60_000, 0) }
        if (++window.count > limit) throw ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many attempts; try again later")
    }
}
