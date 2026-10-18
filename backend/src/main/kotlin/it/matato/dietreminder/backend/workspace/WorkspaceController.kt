package it.matato.dietreminder.backend.workspace

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation

import it.matato.dietreminder.common.model.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Workspace", description = "Complete account snapshots with optimistic version checks")
@RestController
@RequestMapping("/api/workspace")
class WorkspaceController(private val service: WorkspaceService) {
    @GetMapping
    @Operation(summary = "Download the current workspace")
    fun get(@AuthenticationPrincipal account: AccountDto): WorkspaceDto = service.get(account.userId)
    @PutMapping
    @Operation(summary = "Replace the workspace at the expected base version")
    fun put(@AuthenticationPrincipal account: AccountDto, @RequestBody request: WorkspacePutRequest): WorkspaceAck = service.put(account.userId, request)
}
