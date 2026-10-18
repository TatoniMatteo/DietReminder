package it.matato.dietreminder.backend.google

import it.matato.dietreminder.backend.auth.AuthRateLimiter
import it.matato.dietreminder.common.model.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Google")
@RestController
@RequestMapping("/api/auth/google")
class GoogleAuthController(
    private val auth: GoogleAuthService,
    private val challenges: GoogleChallenges,
    private val attempts: AuthRateLimiter
) {
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @PostMapping("/challenge")
    fun challenge(http: HttpServletRequest): GoogleChallengeDto {
        attempts.check(http.remoteAddr)
        return GoogleChallengeDto(challenges.issue("SIGN_IN"))
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @PostMapping
    fun signIn(@RequestBody request: GoogleSignInRequest, http: HttpServletRequest): SessionDto {
        attempts.check(http.remoteAddr)
        return auth.signIn(request)
    }

    @PostMapping("/link")
    fun link(@AuthenticationPrincipal account: AccountDto, @RequestBody request: GoogleSignInRequest): AccountDto =
        auth.link(account.userId, request)
}
