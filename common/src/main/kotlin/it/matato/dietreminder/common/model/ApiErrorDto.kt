package it.matato.dietreminder.common.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(val code: String, val message: String)
