package it.matato.dietreminder.data.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledAlarmInstrumentedTest {
    @Test
    fun testScheduledAlarm() {
        val alarm = ScheduledAlarm(id = 1, type = "MEAL", timeMillis = 1000L, label = "Lunch")
        assertEquals(1, alarm.id)
        assertEquals("Lunch", alarm.label)
    }
}
