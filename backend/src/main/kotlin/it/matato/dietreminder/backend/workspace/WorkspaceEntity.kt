package it.matato.dietreminder.backend.workspace

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "cloud_workspaces")
class WorkspaceEntity(
    @Id val accountId: String,
    var revision: Long = 0,
    @Column(columnDefinition = "TEXT", nullable = false) var payload: String
)

@Entity
@Table(name = "workspace_commits", uniqueConstraints = [UniqueConstraint(name = "uk_workspace_request", columnNames = ["accountId", "requestId"])])
class WorkspaceCommitEntity(
    @Id val id: String,
    val accountId: String,
    val requestId: String,
    val revision: Long,
    @Column(length = 64) val contentHash: String,
    @Column(columnDefinition = "TEXT", nullable = false) val payload: String
)
interface WorkspaceRepository : JpaRepository<WorkspaceEntity, String>
interface WorkspaceCommitRepository : JpaRepository<WorkspaceCommitEntity, String> {
    fun findByAccountIdAndRequestId(accountId: String, requestId: String): WorkspaceCommitEntity?
}
