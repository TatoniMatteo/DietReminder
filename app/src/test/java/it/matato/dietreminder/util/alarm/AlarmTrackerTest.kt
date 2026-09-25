package it.matato.dietreminder.util.alarm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.model.ScheduledAlarm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class AlarmTrackerTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            AlarmTracker.clearAll(context)
        }
    }

    @Test
    fun registerAndGetAlarms_worksCorrectly() = runBlocking {
        val alarm = ScheduledAlarm(id = 101, type = "MEAL", timeMillis = 100000L, label = "Pranzo")
        AlarmTracker.registerAlarm(context, alarm)

        Thread.sleep(100)

        val alarms = AlarmTracker.getAlarms(context)
        assertEquals(1, alarms.size)
        assertEquals(101, alarms[0].id)
    }

    @Test
    fun unregisterAlarm_removesAlarmFromRegistry() = runBlocking {
        val alarm = ScheduledAlarm(id = 102, type = "HYDRATION", timeMillis = 200000L, label = "Idratazione")
        AlarmTracker.registerAlarm(context, alarm)
        Thread.sleep(100)

        AlarmTracker.unregisterAlarm(context, 102)
        Thread.sleep(100)

        val alarms = AlarmTracker.getAlarms(context)
        assertTrue(alarms.isEmpty())
    }
}
