package it.matato.dietreminder.util.notification.receiver

import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestDietApplication::class)
class CourseReceiverTest {
    @Test
    fun queuedAlarmIsSuppressedAfterGlobalDisableAndWorksAfterReenable() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TestDietApplication>()
        val repository = FakeDietRepository()
        app.configRepository = repository
        app.dietRepository = repository
        val notifications = app.getSystemService(NotificationManager::class.java)
        val queued = Intent().putExtra("meal_id", 42L).putExtra("day_name", "MONDAY")
        val receiver = CourseReceiver()

        repository.saveConfig(ConfigKey.MEAL_REMINDERS_ENABLED, "false")
        receiver.process(app, queued)
        assertEquals(0, notifications.activeNotifications.size)

        repository.saveConfig(ConfigKey.MEAL_REMINDERS_ENABLED, "true")
        receiver.process(app, queued)
        assertEquals(1, notifications.activeNotifications.size)
    }
}
