package it.matato.dietreminder.backend.admin

import it.matato.dietreminder.backend.auth.*
import it.matato.dietreminder.backend.backup.GoogleDriveConnectionRepository
import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.backend.workspace.WorkspaceRepository
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.management.ManagementFactory
import java.util.Locale

@Entity
@Table(name = "admin_audit")
class AdminAuditEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    val actorId: String, val action: String, val targetId: String, val createdAt: Long = System.currentTimeMillis()
)

interface AdminAuditRepository : JpaRepository<AdminAuditEntity, Long> {
    fun findTop50ByOrderByCreatedAtDesc(): List<AdminAuditEntity>
}

data class AdminUser(
    val id: String, val email: String, val username: String, val role: String,
    val enabled: Boolean, val googleLinked: Boolean, val createdAt: Long,
    val emailVerified: Boolean, val enrollmentStatus: String, val permissions: Set<it.matato.dietreminder.common.model.AccountPermission>,
    val version: Long, val reviewReason: String?
)

data class AdminUserPage(val users: List<AdminUser>, val total: Long, val page: Int, val pages: Int)
data class AdminOverview(
    val users: Long, val activeUsers: Long, val googleUsers: Long, val workspaces: Long,
    val driveConnections: Long, val uptimeSeconds: Long
)

data class UserStatusRequest(val enabled: Boolean)

@Service
class AdminService(
    private val accounts: AccountRepository, private val tokens: AccountTokenRepository,
    private val audit: AdminAuditRepository, private val workspaces: WorkspaceRepository,
    private val connections: GoogleDriveConnectionRepository
) {
    fun overview() = AdminOverview(
        accounts.count(), accounts.countByEnabled(true), accounts.countByGoogleSubjectIsNotNull(),
        workspaces.count(), connections.count(), ManagementFactory.getRuntimeMXBean().uptime / 1000
    )

    fun users(search: String, page: Int, enrollmentStatus: String = ""): AdminUserPage {
        if (enrollmentStatus.isNotEmpty() && it.matato.dietreminder.common.model.EnrollmentStatus.entries.none { it.name == enrollmentStatus })
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid enrollment status")
        if (search.length > 100 || page !in 0..10000) throw ApiException(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST",
            "Invalid search or page"
        )
        val result = accounts.search(
            "%" + search.trim().lowercase(Locale.ROOT).replace("%", "").replace("_", "") + "%",
            enrollmentStatus, PageRequest.of(page, 20)
        )
        return AdminUserPage(result.content.map { user(it) }, result.totalElements, page, result.totalPages)
    }

    @Transactional
    fun status(actor: String, target: String, enabled: Boolean): AdminUser {
        val account = accounts.lockById(target) ?: throw ApiException(
            HttpStatus.NOT_FOUND,
            "ACCOUNT_NOT_FOUND",
            "Account not found"
        )
        if (actor == target || account.role == "ADMIN") throw ApiException(
            HttpStatus.CONFLICT,
            "ADMIN_PROTECTED",
            "Administrator accounts cannot be suspended here"
        )
        account.enabled = enabled
        if (!enabled) tokens.deleteByAccountId(target)
        audit.save(
            AdminAuditEntity(
                actorId = actor,
                action = if (enabled) "USER_ENABLED" else "USER_SUSPENDED",
                targetId = target
            )
        )
        return user(account)
    }

    @Transactional
    fun revoke(actor: String, target: String) {
        if (!accounts.existsById(target)) throw ApiException(
            HttpStatus.NOT_FOUND,
            "ACCOUNT_NOT_FOUND",
            "Account not found"
        )
        tokens.deleteByAccountId(target)
        audit.save(AdminAuditEntity(actorId = actor, action = "SESSIONS_REVOKED", targetId = target))
    }

    fun audit() = audit.findTop50ByOrderByCreatedAtDesc()
    private fun user(a: AccountEntity) =
        AdminUser(a.id, a.email, a.username, a.role, a.enabled, a.googleSubject != null, a.createdAt,
            a.emailVerified, a.enrollmentStatus, a.toAccountDto().permissions, a.enrollmentVersion, a.reviewReason)
}
