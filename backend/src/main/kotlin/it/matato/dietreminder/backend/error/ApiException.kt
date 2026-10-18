package it.matato.dietreminder.backend.error

import org.springframework.http.HttpStatus

class ApiException(val status: HttpStatus, val code: String, message: String) : RuntimeException(message)
