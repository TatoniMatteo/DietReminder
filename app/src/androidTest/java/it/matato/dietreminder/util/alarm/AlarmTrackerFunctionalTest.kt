package it.matato.dietreminder.util.alarm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.model.ScheduledAlarm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmTrackerFunctionalTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            AlarmTracker.clearAll(context)
        }
    }

    @Test
    fun registerAndGetAlarms_worksOnDevice() = runBlocking {
        val alarm = ScheduledAlarm(id = 201, type = "MEAL", timeMillis = 500000L, label = "Cena")
        AlarmTracker.registerAlarm(context, alarm)

        val alarms = AlarmTracker.getAlarms(context)
        assertNotNull(alarms)
    }
}
