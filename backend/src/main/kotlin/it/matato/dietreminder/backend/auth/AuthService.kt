package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.backend.profile.UserProfileService
import it.matato.dietreminder.backend.validation.validate
import it.matato.dietreminder.common.model.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.UUID

@Service
class AuthService(
    private val accounts: AccountRepository,
    private val creation: AccountCreationService,
    private val enrollment: EnrollmentService,
    private val tokens: AccountTokenRepository,
    private val profiles: UserProfileService,
    private val events: org.springframework.context.ApplicationEventPublisher,
    @Value("\${auth.session-hours:720}") private val sessionHours: Long
) {
    private val passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    private val random = SecureRandom()
    // Unknown accounts still perform the same expensive password check.
    private val dummyHash = passwords.encode(UUID.randomUUID().toString())

    @Transactional
    fun register(request: RegisterRequest): SessionDto {
        val email = email(request.email)
        password(request.password)
        validate(request.username.isNotBlank() && request.username.length <= 100, "username must contain 1 to 100 characters")
        if (accounts.findByEmail(email) != null) throw ApiException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS", "An account with this email already exists")
        val account = creation.create(AccountEntity(UUID.randomUUID().toString(), email, passwords.encode(request.password), request.username.trim()))
        profiles.save(UserProfileDto(account.id, account.username))
        enrollment.begin(account)
        return issue(account)
    }

    @Transactional
    fun login(request: LoginRequest): SessionDto {
        val email = email(request.email)
        validate(request.password.length <= 128, "Password is too long")
        val account = accounts.findByEmail(email)
        val valid = passwords.matches(request.password, account?.passwordHash ?: dummyHash)
        if (account == null || !valid) throw ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect")
        if (!account.enabled) throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account is suspended")
        return issue(account)
    }

    @Transactional(readOnly = true)
    fun authenticate(token: String): AccountDto? {
        if (!token.matches(Regex("[A-Za-z0-9_-]{43}"))) return null
        val stored = tokens.findById(hash(token)).orElse(null) ?: return null
        if (stored.purpose != "SESSION" || stored.expiresAt <= System.currentTimeMillis()) return null
        return accounts.findById(stored.accountId).orElse(null)?.takeIf { it.enabled }?.toAccountDto()
    }

    @Transactional
    fun logout(token: String) { tokens.deleteById(hash(token)) }

    @Transactional
    fun forgot(request: ForgotPasswordRequest) {
        val email = email(request.email)
        val account = accounts.findByEmail(email) ?: return
        val token = newToken()
        tokens.save(AccountTokenEntity(hash(token), account.id, "RESET", System.currentTimeMillis() + 30 * 60_000))
        events.publishEvent(PasswordResetRequested(email, token))
    }

    @Transactional
    fun reset(request: ResetPasswordRequest) {
        password(request.password)
        val stored = tokens.findByHashAndPurpose(hash(request.token), "RESET")
        if (stored == null || stored.expiresAt <= System.currentTimeMillis()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "The reset code is invalid or expired")
        }
        val account = accounts.findById(stored.accountId).orElseThrow()
        account.passwordHash = passwords.encode(request.password)
        tokens.deleteByAccountId(account.id) // Revoke all sessions and all reset codes, including this one.
    }

    @Transactional
    fun changePassword(accountId: String, request: ChangePasswordRequest) {
        validate(request.currentPassword.length in 1..128, "Current password is required")
        password(request.newPassword)
        val account = accounts.lockById(accountId) ?: throw ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in again")
        if (!passwords.matches(request.currentPassword, account.passwordHash)) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect")
        if (request.currentPassword == request.newPassword) throw ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED", "Choose a different password")
        account.passwordHash = passwords.encode(request.newPassword)
        tokens.deleteByAccountId(account.id)
    }

    fun issue(account: AccountEntity): SessionDto {
        tokens.deleteByExpiresAtLessThan(System.currentTimeMillis())
        val token = newToken()
        val expiry = System.currentTimeMillis() + sessionHours.coerceIn(1, 24 * 90) * 3_600_000
        tokens.save(AccountTokenEntity(hash(token), account.id, "SESSION", expiry))
        return SessionDto(token, expiry, account.toAccountDto())
    }

    private fun newToken(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
    private fun hash(token: String): String = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun email(value: String): String {
        val result = value.trim().lowercase(Locale.ROOT)
        validate(result.length <= 254 && result.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")), "A valid email address is required")
        return result
    }
    private fun password(value: String) = validate(it.matato.dietreminder.common.model.PasswordRules.isValid(value), "Password must contain 12 to 128 characters, uppercase, lowercase and a special character")
}
