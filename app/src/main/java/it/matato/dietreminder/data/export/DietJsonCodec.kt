package it.matato.dietreminder.data.export

import kotlinx.serialization.json.Json

class DietJsonCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun encode(diet: DietExport): String =
        json.encodeToString(diet)

    fun decode(json: String): DietExport =
        this.json.decodeFromString(json)
}