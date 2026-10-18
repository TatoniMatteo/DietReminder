package it.matato.dietreminder.util.alarm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.util.UpdateManagerStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
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
		UpdateManagerStatus.isOffline = false
		UpdateManagerStatus.isChecking = false
		runBlocking {
			AlarmTracker.clearAll(context)
		}
	}

	@After
	fun tearDown() {
		UpdateManagerStatus.isOffline = false
		UpdateManagerStatus.isChecking = false
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

	@Test
	fun registerAlarm_worksInOfflineMode() = runBlocking {
		UpdateManagerStatus.isOffline = true
		val alarm = ScheduledAlarm(id = 103, type = "MEAL", timeMillis = 300000L, label = "Cena")

		AlarmTracker.registerAlarm(context, alarm)
		Thread.sleep(100)

		val alarms = AlarmTracker.getAlarms(context)
		assertEquals(listOf(alarm), alarms)
	}
}
