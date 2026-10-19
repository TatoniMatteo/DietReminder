package it.matato.dietreminder.backend.releases

import io.swagger.v3.oas.annotations.tags.Tag
import it.matato.dietreminder.common.model.VersionPolicy
import kotlinx.serialization.json.Json
import org.springframework.core.io.ClassPathResource
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.time.Duration

/** Release metadata ships with the application, alongside Android's offline fallback. */
@Tag(name = "App updates")
@RestController
class ReleasePolicyController {
    private val payload = ClassPathResource("version-policy.json").inputStream.use { it.readBytes() }
    private val policy = Json.decodeFromString<VersionPolicy>(payload.toString(Charsets.UTF_8))
    private val etag = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/api/app/version-policy")
    fun publicPolicy(): ResponseEntity<VersionPolicy> = ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
        .eTag("\"android-$etag\"")
        .body(policy)
}
