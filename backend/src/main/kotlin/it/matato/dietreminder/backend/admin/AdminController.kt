package it.matato.dietreminder.backend.admin

import it.matato.dietreminder.common.model.AccountDto
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Administration")
@RestController
@RequestMapping("/api/admin")
class AdminController(private val admin: AdminService, private val enrollment: it.matato.dietreminder.backend.auth.EnrollmentService) {
    @PostMapping("/users/{id}/review") fun review(@AuthenticationPrincipal actor: AccountDto, @PathVariable id: String,
        @RequestBody request: it.matato.dietreminder.common.model.AccountReviewRequest) = enrollment.review(actor.userId, id, request)
    @GetMapping("/overview") fun overview() = admin.overview()
    @GetMapping("/users") fun users(@RequestParam(defaultValue = "") search: String, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "") enrollmentStatus: String) = admin.users(search, page, enrollmentStatus)
    @PatchMapping("/users/{id}/status") fun status(@AuthenticationPrincipal actor: AccountDto, @PathVariable id: String, @RequestBody request: UserStatusRequest) = admin.status(actor.userId, id, request.enabled)
    @PostMapping("/users/{id}/revoke-sessions") fun revoke(@AuthenticationPrincipal actor: AccountDto, @PathVariable id: String) = admin.revoke(actor.userId, id)
    @GetMapping("/audit") fun audit() = admin.audit()
}
