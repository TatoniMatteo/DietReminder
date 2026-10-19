package it.matato.dietreminder.backend.google

import it.matato.dietreminder.backend.auth.*
import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.backend.profile.UserProfileService
import it.matato.dietreminder.common.model.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GoogleAuthService(private val verifier: GoogleTokenVerifier, private val challenges: GoogleChallenges,
    private val accounts: AccountRepository, private val creation: AccountCreationService, private val profiles: UserProfileService, private val auth: AuthService, private val enrollment: EnrollmentService) {
    @Transactional
    fun signIn(request: GoogleSignInRequest): SessionDto {
        val identity = verifier.verify(request.idToken)
        challenges.consume(identity.nonce, "SIGN_IN")
        var account = accounts.findByGoogleSubject(identity.subject)
        if (account == null) {
            if (accounts.findByEmail(identity.email) != null) throw ApiException(HttpStatus.CONFLICT, "GOOGLE_LINK_REQUIRED", "Sign in with your password and link Google from account settings")
            account = creation.create(AccountEntity(UUID.randomUUID().toString(), identity.email, null, identity.name, googleSubject = identity.subject))
            profiles.save(UserProfileDto(account.id, account.username))
        }
        if (!account.enabled) throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account is suspended")
        enrollment.verifyWithGoogle(account.id)
        return auth.issue(account)
    }
    @Transactional
    fun link(accountId: String, request: GoogleSignInRequest): AccountDto {
        val identity = verifier.verify(request.idToken)
        challenges.consume(identity.nonce, "SIGN_IN")
        val account = accounts.lockById(accountId) ?: throw ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in again")
        if (account.email != identity.email || accounts.findByGoogleSubject(identity.subject)?.id?.let { it != accountId } == true)
            throw ApiException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_MISMATCH", "Choose the Google account with the same email as your Diet Reminder account")
        if (account.googleSubject != null && account.googleSubject != identity.subject)
            throw ApiException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_MISMATCH", "A different Google identity is already linked")
        account.googleSubject = identity.subject
        return account.toAccountDto()
    }
}
