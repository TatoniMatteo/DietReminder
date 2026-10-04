package it.matato.dietreminder.util.alarm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.model.ScheduledAlarm
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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
			awaitAlarms { it.isEmpty() }
		}
	}

	@Test
	fun registerAndGetAlarms_worksOnDevice() = runBlocking {
		val alarm = ScheduledAlarm(id = 201, type = "MEAL", timeMillis = 500000L, label = "Cena")
		AlarmTracker.registerAlarm(context, alarm)

		val alarms = awaitAlarms { registered -> registered.any { it.id == alarm.id } }
		assertEquals(listOf(alarm), alarms)
	}

	private suspend fun awaitAlarms(predicate: (List<ScheduledAlarm>) -> Boolean): List<ScheduledAlarm> =
		withTimeout(2_000) {
			var alarms = AlarmTracker.getAlarms(context)
			while (!predicate(alarms)) {
				delay(10)
				alarms = AlarmTracker.getAlarms(context)
			}
			alarms
		}
}
