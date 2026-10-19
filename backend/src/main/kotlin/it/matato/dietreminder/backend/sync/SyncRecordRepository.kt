package it.matato.dietreminder.backend.sync

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable

@Repository
interface SyncRecordRepository : JpaRepository<SyncRecordEntity, Long> {
    fun findByUserIdAndIdGreaterThanOrderByIdAsc(
        userId: String,
        afterId: Long,
        pageable: Pageable
    ): List<SyncRecordEntity>

    fun findByUserIdAndRequestId(userId: String, requestId: String): SyncRecordEntity?
}
