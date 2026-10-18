package it.matato.dietreminder.backend.error

import it.matato.dietreminder.common.model.ApiErrorDto
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun api(ex: ApiException): ResponseEntity<ApiErrorDto> =
        ResponseEntity.status(ex.status).body(ApiErrorDto(ex.code, ex.message ?: ex.code))

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun malformed(): ResponseEntity<ApiErrorDto> = ResponseEntity.badRequest()
        .body(ApiErrorDto("INVALID_REQUEST", "Invalid JSON body or query parameter"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun conflict(): ResponseEntity<ApiErrorDto> = ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiErrorDto("DATA_CONFLICT", "The request conflicts with existing data"))
}
