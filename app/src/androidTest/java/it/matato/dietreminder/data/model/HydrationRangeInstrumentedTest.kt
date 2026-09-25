package it.matato.dietreminder.data.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HydrationRangeInstrumentedTest {
    @Test
    fun testHydrationRange() {
        val range = HydrationRange(startMinutes = 480, endMinutes = 1200)
        assertEquals(480, range.startMinutes)
        assertEquals(1200, range.endMinutes)
    }
}
