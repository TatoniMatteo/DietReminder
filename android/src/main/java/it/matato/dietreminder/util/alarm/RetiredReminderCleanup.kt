package it.matato.dietreminder.util.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

/** Compatibility cleanup for reminders installed by older app versions. */
internal object RetiredReminderCleanup {
    fun run(context: Context) {
        val intent = Intent().setClassName(
            context.packageName,
            "it.matato.dietreminder.util.notification.receiver.HydrationReceiver"
        )
        PendingIntent.getBroadcast(
            context, 999, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { pendingIntent ->
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
            pendingIntent.cancel()
        }
        context.getSystemService(NotificationManager::class.java).apply {
            cancel(999)
            deleteNotificationChannel("hydration_alarm")
        }
        context.getSharedPreferences("notification_channels", Context.MODE_PRIVATE).edit {
            remove("channel_version_hydration_alarm")
        }
    }
}
