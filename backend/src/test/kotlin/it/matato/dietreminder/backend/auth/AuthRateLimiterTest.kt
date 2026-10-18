package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.backend.error.ApiException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AuthRateLimiterTest {
    @Test
    fun `authentication attempts are limited per remote address`() {
        val limiter = AuthRateLimiter(2)
        limiter.check("address-a")
        limiter.check("address-a")
        val ex = assertThrows(ApiException::class.java) { limiter.check("address-a") }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.status)
        limiter.check("address-b")
    }
}
