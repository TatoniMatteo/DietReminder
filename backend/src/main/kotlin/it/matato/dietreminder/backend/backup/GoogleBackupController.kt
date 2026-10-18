package it.matato.dietreminder.backend.backup

import it.matato.dietreminder.backend.google.GoogleTokenVerifier
import it.matato.dietreminder.common.model.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Google Drive backups")
@RestController
class GoogleBackupController(
    private val accounts: it.matato.dietreminder.backend.auth.AccountRepository,private val backups: GoogleBackupService, private val google: GoogleTokenVerifier) {
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/api/auth/providers") fun providers() = GoogleProvidersDto(google.clientId, google.clientId.isNotBlank(), backups.available, !accounts.existsByIdIsNotNull())
    @GetMapping("/api/backups/google/status") fun status(@AuthenticationPrincipal account: AccountDto) = backups.status(account.userId)
    @PostMapping("/api/backups/google/authorize") fun authorize(@AuthenticationPrincipal account: AccountDto) = backups.start(account.userId)
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/api/google/drive/callback", produces = ["text/html"])
    fun callback(@RequestParam(required = false) code: String?, @RequestParam(required = false) state: String?, @RequestParam(required = false) error: String?): ResponseEntity<String> {
        if (error != null || code == null || state == null) return page("Collegamento annullato", "Puoi tornare all’app e riprovare quando vuoi.")
        backups.connect(code, state)
        return page("Google Drive collegato", "Puoi chiudere questa finestra e tornare a Diet Reminder.")
    }
    @DeleteMapping("/api/backups/google/connection") fun disconnect(@AuthenticationPrincipal account: AccountDto) = backups.disconnect(account.userId)
    @GetMapping("/api/backups/google") fun list(@AuthenticationPrincipal account: AccountDto) = backups.list(account.userId)
    @PostMapping("/api/backups/google") fun create(@AuthenticationPrincipal account: AccountDto) = backups.create(account.userId)
    @PostMapping("/api/backups/google/restore") fun restore(@AuthenticationPrincipal account: AccountDto, @RequestBody request: GoogleRestoreRequest) = backups.restore(account.userId, request)
    @DeleteMapping("/api/backups/google/{id}") fun delete(@AuthenticationPrincipal account: AccountDto, @PathVariable id: String) = backups.delete(account.userId, id)
    private fun page(title: String, message: String) = ResponseEntity.ok().header("Cache-Control", "no-store").header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'")
        .body("<!doctype html><html lang='it'><meta charset='utf-8'><meta name='viewport' content='width=device-width'><title>$title</title><body style='background:#0d0f14;color:#e5e7ee;font:18px system-ui;padding:10%;line-height:1.6'><h1>$title</h1><p>$message</p></body></html>")
}
