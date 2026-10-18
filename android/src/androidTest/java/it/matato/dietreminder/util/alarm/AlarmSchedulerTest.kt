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
class AlarmSchedulerTest {

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
	fun cancelAlarm_removesAlarmFromLocalRegistry() = runBlocking {
		val alarm = ScheduledAlarm(
			id = 4,
			type = "DINNER",
			timeMillis = System.currentTimeMillis() + 60_000,
			label = "Dinner",
		)
		AlarmTracker.registerAlarm(context, alarm)
		awaitAlarms { alarms -> alarms.contains(alarm) }

		AlarmScheduler.cancelAlarm(context, alarm.id)

		val remaining = awaitAlarms { alarms -> alarms.none { it.id == alarm.id } }
		assertEquals(emptyList<ScheduledAlarm>(), remaining)
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
