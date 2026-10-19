package it.matato.dietreminder.backend.auth

import jakarta.persistence.*
import it.matato.dietreminder.common.model.AccountPermission

@Entity
@Table(name = "accounts")
class AccountEntity(
    @Id val id: String,
    @Column(unique = true, nullable = false, length = 254) val email: String,
    @Column var passwordHash: String?,
    @Column(nullable = false, length = 100) var username: String,
    @Column(unique = true) var googleSubject: String? = null,
    @Column(nullable = false, length = 16) var role: String = "USER",
    @Column(nullable = false) var enabled: Boolean = true,
    @Column(nullable = false) var emailVerified: Boolean = false,
    @Column(nullable = false, length = 32) var enrollmentStatus: String = "PENDING_EMAIL",
    @Column(nullable = false, columnDefinition = "TEXT") var permissions: String = "WORKSPACE_READ,DIETS_WRITE,SHOPPING_WRITE,PROFILE_WRITE,BACKUP_MANAGE",
    @Column(length = 1000) var reviewReason: String? = null,
    @Column(nullable = false) var enrollmentVersion: Long = 0,
    @Column(nullable = false) var adminIntroductionAcknowledged: Boolean = false,
    @Column(nullable = false) val createdAt: Long = System.currentTimeMillis()
)

@Entity
@Table(name = "account_tokens", indexes = [Index(name = "idx_token_account", columnList = "accountId")])
class AccountTokenEntity(
    @Id @Column(length = 64) val hash: String,
    @Column(nullable = false) val accountId: String,
    @Column(nullable = false) val purpose: String,
    @Column(nullable = false) val expiresAt: Long
)

fun AccountEntity.toAccountDto() = it.matato.dietreminder.common.model.AccountDto(
    id, email, username, role, googleSubject != null, emailVerified,
    it.matato.dietreminder.common.model.EnrollmentStatus.valueOf(enrollmentStatus),
    permissions.split(',').filter { it.isNotBlank() }.map { AccountPermission.valueOf(it) }.toSet(),
    reviewReason, enrollmentVersion, role == "ADMIN" && !adminIntroductionAcknowledged
)
