package it.matato.dietreminder.backend.auth

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements

import it.matato.dietreminder.common.model.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Accounts", description = "Registration, sessions and password recovery")
@RestController
@RequestMapping("/api/auth")
class AuthController(private val auth: AuthService, private val attempts: AuthRateLimiter, private val mail: PasswordResetMail) {
    @PostMapping("/register")
    @Operation(summary = "Create an account and session")
    @SecurityRequirements
    fun register(@RequestBody request: RegisterRequest, http: HttpServletRequest): SessionDto {
        attempts.check(http.remoteAddr)
        return auth.register(request)
    }
    @PostMapping("/login")
    @Operation(summary = "Sign in and create a session")
    @SecurityRequirements
    fun login(@RequestBody request: LoginRequest, http: HttpServletRequest): SessionDto {
        attempts.check(http.remoteAddr)
        return auth.login(request)
    }
    @GetMapping("/me")
    @Operation(summary = "Get the signed-in account")
    fun me(@AuthenticationPrincipal account: AccountDto): AccountDto = account

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke the current session")
    fun logout(http: HttpServletRequest) { auth.logout(http.getHeader("Authorization").removePrefix("Bearer ")) }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change your password after verifying the current password; revokes all sessions")
    fun changePassword(@AuthenticationPrincipal account: AccountDto, @RequestBody request: ChangePasswordRequest, http: HttpServletRequest) {
        attempts.check(http.remoteAddr)
        attempts.check("change-password:${account.userId}")
        auth.changePassword(account.userId, request)
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request a password recovery email (requires SMTP)")
    @SecurityRequirements
    fun forgot(@RequestBody request: ForgotPasswordRequest, http: HttpServletRequest) {
        attempts.check(http.remoteAddr)
        mail.ensureAvailable() // Same result for existing and unknown emails when SMTP is not configured.
        auth.forgot(request)
    }
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reset a password using the emailed code")
    @SecurityRequirements
    fun reset(@RequestBody request: ResetPasswordRequest, http: HttpServletRequest) {
        attempts.check(http.remoteAddr)
        auth.reset(request)
    }
}
