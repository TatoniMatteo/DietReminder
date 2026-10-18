package it.matato.dietreminder.backend.validation

import org.springframework.http.HttpStatus
import it.matato.dietreminder.backend.error.ApiException

internal fun validate(condition: Boolean, message: String) {
    if (!condition) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message)
}

internal fun validateUserId(userId: String) =
    validate(userId.matches(Regex("[A-Za-z0-9_-]{1,128}")), "userId must contain 1 to 128 letters, digits, underscores or hyphens")
