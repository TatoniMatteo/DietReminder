package it.matato.dietreminder.backend.sync

import it.matato.dietreminder.backend.validation.*
import it.matato.dietreminder.backend.error.ApiException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import it.matato.dietreminder.common.model.SyncRecordDto
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class SyncService(private val records: SyncRecordRepository, private val mapper: ObjectMapper) {
    fun push(record: SyncRecordDto): SyncRecordDto {
        validateUserId(record.userId)
        validate(
            record.dataType.matches(Regex("[A-Z][A-Z0-9_]{0,63}")),
            "dataType must be an uppercase identifier of at most 64 characters"
        )
        validate(
            record.requestId?.matches(Regex("[A-Za-z0-9_-]{1,64}")) != false,
            "requestId must contain 1 to 64 letters, digits, underscores or hyphens"
        )
        validate(record.payloadJson.toByteArray(Charsets.UTF_8).size <= 1_048_576, "payloadJson must not exceed 1 MiB")
        val payload = try {
            mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(record.payloadJson)
        } catch (_: JsonProcessingException) {
            null
        }
        validate(payload != null && (payload.isObject || payload.isArray), "payloadJson must be a JSON object or array")
        record.requestId?.let { requestId ->
            records.findByUserIdAndRequestId(record.userId, requestId)?.let { return replay(it, record) }
        }
        // Repository transactions finish before this catch, allowing concurrent retries to be read safely.
        return try {
            records.saveAndFlush(
                SyncRecordEntity(
                    userId = record.userId, dataType = record.dataType, payload = record.payloadJson,
                    requestId = record.requestId
                )
            ).toDto()
        } catch (ex: DataIntegrityViolationException) {
            val existing = record.requestId?.let { records.findByUserIdAndRequestId(record.userId, it) }
            if (existing != null) replay(existing, record) else throw ex
        }
    }

    fun pull(userId: String, afterId: Long, limit: Int): List<SyncRecordDto> {
        validateUserId(userId)
        validate(afterId >= 0, "afterId must be nonnegative")
        validate(limit in 1..500, "limit must be between 1 and 500")
        return records.findByUserIdAndIdGreaterThanOrderByIdAsc(userId, afterId, PageRequest.of(0, limit))
            .map { it.toDto() }
    }

    private fun replay(existing: SyncRecordEntity, record: SyncRecordDto): SyncRecordDto {
        if (existing.dataType != record.dataType || existing.payload != record.payloadJson) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "REQUEST_ID_CONFLICT",
                "requestId was already used for different data"
            )
        }
        return existing.toDto()
    }

    private fun SyncRecordEntity.toDto() = SyncRecordDto(
        id = id, userId = userId, dataType = dataType, payloadJson = payload,
        timestamp = timestamp, requestId = requestId
    )
}
