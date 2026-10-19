package it.matato.dietreminder.util.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import it.matato.dietreminder.TestDietApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestDietApplication::class)
class RetiredReminderCleanupTest {
    @Test
    fun cleanupCancelsOnlyTheRetiredReminderAndIsRepeatable() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        val intent = Intent().setClassName(
            context.packageName,
            "it.matato.dietreminder.util.notification.receiver.HydrationReceiver"
        )
        val old = PendingIntent.getBroadcast(context, 999, intent, PendingIntent.FLAG_IMMUTABLE)
        val meal = PendingIntent.getBroadcast(context, 0, Intent("meal-test"), PendingIntent.FLAG_IMMUTABLE)
        alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000, old)
        alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000, meal)
        notifications.createNotificationChannel(
            NotificationChannel(
                "hydration_alarm",
                "Old",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notifications.createNotificationChannel(
            NotificationChannel(
                "default_alarms",
                "Meals",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        repeat(2) { RetiredReminderCleanup.run(context) }
        assertNull(
            PendingIntent.getBroadcast(
                context,
                999,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        )
        assertEquals(listOf(meal), shadowOf(alarms).scheduledAlarms.map { it.operation })
        assertNull(notifications.getNotificationChannel("hydration_alarm"))
        assertNotNull(notifications.getNotificationChannel("default_alarms"))
    }
}
