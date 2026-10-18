package it.matato.dietreminder.backend.sync

import jakarta.persistence.*

@Entity
@Table(
    name = "sync_records",
    uniqueConstraints = [UniqueConstraint(name = "uk_sync_user_request", columnNames = ["userId", "requestId"])],
    indexes = [Index(name = "idx_sync_user_id", columnList = "userId,id")]
)
data class SyncRecordEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val userId: String,
    val dataType: String, // e.g. "DIET", "MEAL", "SHOPPING_LIST"
    @Column(columnDefinition = "TEXT")
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    @Column(length = 64)
    val requestId: String? = null
)
