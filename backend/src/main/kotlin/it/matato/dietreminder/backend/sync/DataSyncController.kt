package it.matato.dietreminder.backend.sync

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation

import it.matato.dietreminder.common.model.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Sync records", description = "Append-only record API, independent of workspace synchronization")
@RestController
@RequestMapping("/api/user/sync")
class DataSyncController(private val sync: SyncService) {
    @PostMapping("/push")
    @Operation(summary = "Append a synchronization record")
    fun pushSyncData(
        @AuthenticationPrincipal account: AccountDto,
        @RequestBody record: SyncPushRequest
    ): SyncRecordDto =
        sync.push(
            SyncRecordDto(
                userId = account.userId,
                dataType = record.dataType,
                payloadJson = record.payloadJson,
                requestId = record.requestId
            )
        )

    @GetMapping("/pull")
    @Operation(summary = "Read your synchronization records")
    fun pullSyncData(
        @AuthenticationPrincipal account: AccountDto,
        @RequestParam(defaultValue = "0") afterId: Long,
        @RequestParam(defaultValue = "100") limit: Int
    ): List<SyncRecordDto> = sync.pull(account.userId, afterId, limit)
}
