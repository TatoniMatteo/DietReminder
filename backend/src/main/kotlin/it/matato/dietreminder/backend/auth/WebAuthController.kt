package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.backend.google.GoogleAuthService
import it.matato.dietreminder.backend.google.googleHash
import it.matato.dietreminder.common.model.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag

const val WEB_SESSION_COOKIE = "dietreminder_session"
fun webCsrf(token: String) = googleHash("dietreminder-web-csrf:$token")
data class WebSessionDto(val account: AccountDto, val csrfToken: String)

@Tag(name = "Web sessions", description = "HttpOnly session cookies with CSRF protection")
@RestController
@RequestMapping("/api/web/auth")
class WebAuthController(
    private val auth: AuthService, private val google: GoogleAuthService,
    private val attempts: AuthRateLimiter, @Value("\${web.secure-cookies:true}") private val secureCookies: Boolean
) {
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        http: HttpServletRequest,
        response: HttpServletResponse
    ): WebSessionDto {
        attempts.check(http.remoteAddr)
        return cookie(auth.login(request), response)
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterRequest,
        http: HttpServletRequest,
        response: HttpServletResponse
    ): WebSessionDto {
        attempts.check(http.remoteAddr)
        return cookie(auth.register(request), response)
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @PostMapping("/google")
    fun google(
        @RequestBody request: GoogleSignInRequest,
        http: HttpServletRequest,
        response: HttpServletResponse
    ): WebSessionDto {
        attempts.check(http.remoteAddr)
        return cookie(google.signIn(request), response)
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal account: AccountDto, http: HttpServletRequest): WebSessionDto =
        WebSessionDto(
            account,
            webCsrf(
                http.cookies?.firstOrNull { it.name == WEB_SESSION_COOKIE }?.value
                ?: throw it.matato.dietreminder.backend.error.ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHENTICATED",
                    "A web session is required"
                )
            )
        )

    @PostMapping("/logout")
    fun logout(http: HttpServletRequest, response: HttpServletResponse) {
        http.cookies?.firstOrNull { it.name == WEB_SESSION_COOKIE }?.let { auth.logout(it.value) }
        response.addHeader(
            "Set-Cookie",
            ResponseCookie.from(WEB_SESSION_COOKIE, "").httpOnly(true).secure(secureCookies).sameSite("Lax")
                .path("/api").maxAge(0).build().toString()
        )
    }

    private fun cookie(session: SessionDto, response: HttpServletResponse): WebSessionDto {
        response.addHeader(
            "Set-Cookie",
            ResponseCookie.from(WEB_SESSION_COOKIE, session.token).httpOnly(true).secure(secureCookies).sameSite("Lax")
                .path("/api").maxAge((session.expiresAt - System.currentTimeMillis()) / 1000).build().toString()
        )
        response.setHeader("Cache-Control", "no-store")
        return WebSessionDto(session.account, webCsrf(session.token))
    }
}
