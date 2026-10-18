package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.common.model.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class EnrollmentController(private val enrollment: EnrollmentService, private val attempts: AuthRateLimiter) {
    @GetMapping("/enrollment") fun status(@AuthenticationPrincipal account: AccountDto) = enrollment.status(account.userId)
    @PostMapping("/verify-email")
    fun verify(@AuthenticationPrincipal account: AccountDto, @RequestBody request: VerifyEmailRequest, http: HttpServletRequest): AccountDto {
        attempts.check("email-verification:${account.userId}")
        attempts.check(http.remoteAddr)
        return enrollment.verify(account.userId, request)
    }
    @PostMapping("/resend-verification")
    fun resend(@AuthenticationPrincipal account: AccountDto, http: HttpServletRequest) {
        attempts.check(http.remoteAddr)
        enrollment.resend(account.userId)
    }
    @PostMapping("/admin-introduction") fun acknowledge(@AuthenticationPrincipal account: AccountDto) = enrollment.acknowledgeIntroduction(account.userId)
}
