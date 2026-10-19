package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.common.model.*
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Service
class EnrollmentService(
    private val accounts: AccountRepository,
    private val tokens: AccountTokenRepository,
    private val jdbc: JdbcTemplate,
    private val mail: AccountNotificationMail
) {
    @Transactional
    fun begin(account: AccountEntity) {
        mail.ensureAvailable()
        event(account, "Conferma la tua email per proseguire. / Verify your email to continue.")
        verification(account)
    }

    /** Called only after signature, audience, nonce and email_verified validation by Google. */
    @Transactional
    fun verifyWithGoogle(id: String) {
        val account = accounts.lockById(id) ?: throw unauthenticated()
        if (account.emailVerified) return
        mail.ensureAvailable()
        account.emailVerified = true
        jdbc.update("DELETE FROM account_tokens WHERE account_id = ? AND purpose = 'VERIFY_EMAIL'", id)
        jdbc.update("DELETE FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL'", id)
        verified(account)
    }

    @Transactional(readOnly = true)
    fun status(id: String): EnrollmentDto {
        val account = accounts.findById(id).orElseThrow()
        val history = jdbc.query("SELECT id, status, message, created_at FROM enrollment_events WHERE account_id = ? ORDER BY id", { rs, _ ->
            EnrollmentEventDto(rs.getLong("id"), EnrollmentStatus.valueOf(rs.getString("status")), rs.getString("message"), rs.getLong("created_at"))
        }, id)
        return EnrollmentDto(account.toAccountDto(), history)
    }

    @Transactional
    fun verify(id: String, request: VerifyEmailRequest): AccountDto {
        val account = accounts.lockById(id) ?: throw unauthenticated()
        if (account.emailVerified) throw ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", "Email already verified")
        if (!request.token.matches(Regex("[A-Za-z0-9_-]{43}"))) throw invalidCode()
        val token = tokens.findByHashAndPurpose(hash(request.token), "VERIFY_EMAIL") ?: throw invalidCode()
        if (token.accountId != id || token.expiresAt <= System.currentTimeMillis()) throw invalidCode()
        account.emailVerified = true
        // Discard all outstanding verification codes and scrub queued token text.
        jdbc.update("DELETE FROM account_tokens WHERE account_id = ? AND purpose = 'VERIFY_EMAIL'", id)
        jdbc.update("DELETE FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL'", id)
        verified(account)
        return account.toAccountDto()
    }

    @Transactional
    fun resend(id: String) {
        val account = accounts.lockById(id) ?: throw unauthenticated()
        if (account.emailVerified) throw ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", "Email already verified")
        mail.ensureAvailable()
        val latest = jdbc.queryForObject("SELECT MAX(created_at) FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL'", Long::class.java, id)
        if (latest != null && latest > System.currentTimeMillis() - 60_000) throw ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Wait before requesting another email")
        verification(account)
    }

    @Transactional
    fun review(actorId: String, id: String, request: AccountReviewRequest): AccountDto {
        val account = accounts.lockById(id) ?: throw ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found")
        if (account.role == "ADMIN") throw ApiException(HttpStatus.CONFLICT, "ADMIN_PROTECTED", "Administrator accounts are protected")
        if (!account.enabled) throw ApiException(HttpStatus.CONFLICT, "ACCOUNT_DISABLED", "Reactivate this account before reviewing it")
        if (!account.emailVerified) throw ApiException(HttpStatus.CONFLICT, "EMAIL_NOT_VERIFIED", "Email must be verified before review")
        if (account.enrollmentVersion != request.version) throw ApiException(HttpStatus.CONFLICT, "REVIEW_CONFLICT", "This application changed; reload before reviewing")
        if (account.enrollmentStatus !in listOf("PENDING_APPROVAL", "APPROVED")) throw ApiException(HttpStatus.CONFLICT, "INVALID_REVIEW_STATE", "This application cannot be reviewed")
        if (request.reason.length > 1000 || (!request.approved && request.reason.isBlank())) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "A rejection reason is required (maximum 1000 characters)")
        if (request.approved && AccountPermission.WORKSPACE_READ !in request.permissions) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Workspace read access is required")
        mail.ensureAvailable()
        account.enrollmentStatus = if (request.approved) "APPROVED" else "REJECTED"
        account.permissions = if (request.approved) request.permissions.sortedBy { it.ordinal }.joinToString(",") else ""
        account.reviewReason = request.reason.trim().ifBlank { null }
        account.enrollmentVersion++
        val message = if (request.approved) "Richiesta approvata. Puoi usare l’app con i permessi assegnati. / Approved: you can now use the app with your assigned permissions."
            else "Richiesta rifiutata / Application rejected: ${account.reviewReason}"
        event(account, message)
        mail.enqueue(account, "REVIEW", "Diet Reminder - ${if (request.approved) "Account approvato / Approved" else "Richiesta rifiutata / Rejected"}", message)
        jdbc.update("INSERT INTO admin_audit(actor_id, action, target_id, created_at) VALUES (?, ?, ?, ?)", actorId,
            if (request.approved) "ACCOUNT_APPROVED" else "ACCOUNT_REJECTED", id, System.currentTimeMillis())
        return account.toAccountDto()
    }

    @Transactional
    fun acknowledgeIntroduction(id: String): AccountDto {
        val account = accounts.lockById(id) ?: throw unauthenticated()
        if (account.role != "ADMIN" || !account.emailVerified || account.enrollmentStatus != "APPROVED") throw ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "An active administrator is required")
        if (!account.adminIntroductionAcknowledged) {
            account.adminIntroductionAcknowledged = true
            account.enrollmentVersion++
        }
        return account.toAccountDto()
    }

    private fun verified(account: AccountEntity) {
        account.enrollmentStatus = if (account.role == "ADMIN") "APPROVED" else "PENDING_APPROVAL"
        account.enrollmentVersion++
        val message = if (account.role == "ADMIN") "Email verificata. Sei l’amministratore iniziale. / Email verified. You are the initial administrator."
            else "Email verificata. La richiesta è in attesa dell’amministratore. / Email verified. Your application is awaiting administrator approval."
        event(account, message)
        mail.enqueue(account, "STATUS", "Diet Reminder - Stato registrazione / Registration status", message)
    }

    private fun verification(account: AccountEntity) {
        jdbc.update("DELETE FROM account_tokens WHERE account_id = ? AND purpose = 'VERIFY_EMAIL'", account.id)
        jdbc.update("DELETE FROM account_mail_outbox WHERE account_id = ? AND kind = 'VERIFY_EMAIL'", account.id)
        val code = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))
        tokens.save(AccountTokenEntity(hash(code), account.id, "VERIFY_EMAIL", System.currentTimeMillis() + 24 * 60 * 60_000))
        mail.enqueue(account, "VERIFY_EMAIL", "Diet Reminder - Verifica email / Verify email",
            "Conferma la tua email inserendo questo codice nell’app. Scade tra 24 ore.\nEnter this code in the app to verify your email. It expires in 24 hours.\n\n$code")
    }

    private fun event(account: AccountEntity, message: String) = jdbc.update(
        "INSERT INTO enrollment_events(account_id, status, message, created_at) VALUES (?, ?, ?, ?)", account.id, account.enrollmentStatus, message, System.currentTimeMillis())
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun invalidCode() = ApiException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN", "The verification code is invalid or expired")
    private fun unauthenticated() = ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in again")
}
