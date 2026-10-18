package it.matato.dietreminder.backend.releases

import it.matato.dietreminder.backend.admin.AdminAuditEntity
import it.matato.dietreminder.backend.admin.AdminAuditRepository
import it.matato.dietreminder.backend.error.ApiException
import it.matato.dietreminder.common.model.*
import jakarta.persistence.*
import kotlinx.serialization.json.Json
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.CacheControl
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag
import java.net.URI
import java.time.Duration

@Entity
@Table(name = "app_release_policy")
class ReleasePolicyEntity(@Id val id: String = "android", var revision: Long = 1, @Column(columnDefinition = "TEXT") var payload: String)
interface ReleasePolicyRepository : JpaRepository<ReleasePolicyEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) fun findByIdAndRevision(id: String, revision: Long): ReleasePolicyEntity?
}
data class ManagedVersionPolicy(val policy: VersionPolicy, val revision: Long)

@Service
class ReleasePolicyService(private val repository: ReleasePolicyRepository, private val audit: AdminAuditRepository) {
    private val json = Json { encodeDefaults = true }
    fun get(): ManagedVersionPolicy {
        val row = repository.findById("android").orElseThrow()
        return ManagedVersionPolicy(json.decodeFromString<VersionPolicy>(row.payload), row.revision)
    }
    @Transactional
    fun update(actor: String, request: ManagedVersionPolicy): ManagedVersionPolicy {
        val p = request.policy
        val uri = runCatching { URI(p.updateUrl) }.getOrNull()
        if (!(p.minSupportedVersionCode >= 1 && p.deprecatedVersionCode >= p.minSupportedVersionCode && p.latestVersionCode >= p.deprecatedVersionCode && p.minSupportedMajor >= 1 && p.minSupportedMajor <= (p.latestVersionName.substringBefore('.').toIntOrNull() ?: 0)
            && p.latestVersionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?")) && uri?.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && p.updateUrl.length <= 2048))
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_RELEASE_POLICY", "Invalid version bounds, version name or HTTPS download URL")
        val row = repository.findByIdAndRevision("android", request.revision) ?: throw ApiException(HttpStatus.CONFLICT, "RELEASE_POLICY_CONFLICT", "Another administrator changed the release policy; reload before saving")
        row.payload = json.encodeToString(VersionPolicy.serializer(), p)
        row.revision++
        audit.save(AdminAuditEntity(actorId = actor, action = "RELEASE_POLICY_UPDATED", targetId = "android"))
        return ManagedVersionPolicy(p, row.revision)
    }
}
@Tag(name = "App updates")
@RestController
class ReleasePolicyController(private val policies: ReleasePolicyService) {
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/api/app/version-policy")
    fun publicPolicy(): ResponseEntity<VersionPolicy> {
        val result = policies.get()
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5))).eTag("\"android-${result.revision}\"").body(result.policy)
    }
    @GetMapping("/api/admin/version-policy") fun get() = policies.get()
    @PutMapping("/api/admin/version-policy") fun update(@AuthenticationPrincipal account: AccountDto, @RequestBody request: ManagedVersionPolicy) = policies.update(account.userId, request)
}
