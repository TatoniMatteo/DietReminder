package it.matato.dietreminder.data.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietJsonCodecTest {

    @Test
    fun testJsonCodecEncodeDecode() {
        val codec = DietJsonCodec()
        val export = DietExport(
            uuid = "123",
            name = "Test Diet",
            nextMealWindowMinutes = 60,
            meals = emptyList()
        )
        val json = codec.encode(export)
        val decoded = codec.decode(json)
        assertEquals(export.name, decoded.name)
    }
}
