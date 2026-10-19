package it.matato.dietreminder.backend.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType

interface AccountRepository : JpaRepository<AccountEntity, String> {
    fun existsByIdIsNotNull(): Boolean
    fun findByEmail(email: String): AccountEntity?
    fun findByGoogleSubject(googleSubject: String): AccountEntity?
    fun countByEnabled(enabled: Boolean): Long
    fun countByGoogleSubjectIsNotNull(): Long
    @org.springframework.data.jpa.repository.Query("select a from AccountEntity a where (lower(a.email) like :search or lower(a.username) like :search) and (:status = '' or a.enrollmentStatus = :status) order by a.createdAt desc")
    fun search(@org.springframework.data.repository.query.Param("search") search: String, @org.springframework.data.repository.query.Param("status") status: String, page: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<AccountEntity>
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from AccountEntity a where a.id = :id")
    fun lockById(@org.springframework.data.repository.query.Param("id") id: String): AccountEntity?
}
interface AccountTokenRepository : JpaRepository<AccountTokenEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByHashAndPurpose(hash: String, purpose: String): AccountTokenEntity?
    fun deleteByAccountId(accountId: String)
    fun deleteByExpiresAtLessThan(now: Long)
}
