package it.matato.dietreminder.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class HydrationRangeTest {

    @Test
    fun hydrationRange_serializesAndDeserializesCorrectly() {
        val range = HydrationRange(startMinutes = 480, endMinutes = 1200)
        val json = Json.encodeToString(range)

        val decoded = Json.decodeFromString<HydrationRange>(json)
        assertEquals(480, decoded.startMinutes)
        assertEquals(1200, decoded.endMinutes)
    }
}
