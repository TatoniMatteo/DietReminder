package it.matato.dietreminder.backend.auth

import it.matato.dietreminder.common.model.*

/** Restricted sessions may only follow their enrollment and sign out. */
object AccountAccess {
    private val enrollmentPaths = setOf("/api/auth/me", "/api/web/auth/me", "/api/auth/enrollment",
        "/api/auth/verify-email", "/api/auth/resend-verification", "/api/auth/logout", "/api/web/auth/logout")

    fun denial(account: AccountDto, path: String, method: String): ApiErrorDto? {
        if (path in enrollmentPaths || path in setOf("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password",
            "/api/auth/providers", "/api/auth/google", "/api/auth/google/challenge", "/api/web/auth/register", "/api/web/auth/login", "/api/web/auth/google",
            "/api/app/version-policy", "/api/app/shopping-categories", "/api/google/drive/callback") || path.startsWith("/actuator/health") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) return null
        if (!account.ready) return ApiErrorDto("ACCOUNT_NOT_READY", "Complete email verification and administrator approval")
        if (path.startsWith("/api/admin/")) return null // Role check remains in Spring Security.
        val required = when {
            path == "/api/workspace" -> setOf(AccountPermission.WORKSPACE_READ)
            path.startsWith("/api/user/sync") && method != "GET" -> setOf(AccountPermission.DIETS_WRITE, AccountPermission.SHOPPING_WRITE)
            path.startsWith("/api/user/sync") -> setOf(AccountPermission.WORKSPACE_READ)
            path == "/api/user/profile" && method != "GET" -> setOf(AccountPermission.PROFILE_WRITE)
            path == "/api/user/profile" -> setOf(AccountPermission.WORKSPACE_READ)
            path == "/api/backups/google/restore" -> setOf(AccountPermission.BACKUP_MANAGE, AccountPermission.DIETS_WRITE, AccountPermission.SHOPPING_WRITE, AccountPermission.PROFILE_WRITE)
            path.startsWith("/api/backups/") -> setOf(AccountPermission.BACKUP_MANAGE)
            else -> emptySet()
        }
        return if (required.all(account::permits)) null else ApiErrorDto("PERMISSION_DENIED", "Your assigned permissions do not allow this action")
    }
}
