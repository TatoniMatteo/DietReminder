package it.matato.dietreminder.util.shopping

import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import it.matato.dietreminder.MainActivity
import it.matato.dietreminder.R
import it.matato.dietreminder.common.model.ShoppingPlanning
import it.matato.dietreminder.common.model.WorkspaceSnapshot
import it.matato.dietreminder.util.notification.NotificationChannelConfig
import it.matato.dietreminder.util.notification.NotificationChannelManager
import it.matato.dietreminder.util.notification.NotificationHelper
import java.time.LocalDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ShoppingReminderNotifier {
    private val lock = Mutex()
    suspend fun deliver(context: Context, snapshot: WorkspaceSnapshot, scope: String, now: LocalDateTime, isCurrent: () -> Boolean): Int = lock.withLock {
        val manager = context.getSystemService(NotificationManager::class.java)
        NotificationChannelManager(context).ensureChannel(NotificationChannelConfig(id = ShoppingReminderWorker.CHANNEL, nameResId = R.string.shopping_reminders_channel, descriptionResId = R.string.shopping_reminders_description, importance = NotificationManager.IMPORTANCE_DEFAULT))
        if (!isCurrent() || !NotificationManagerCompat.from(context).areNotificationsEnabled() || manager.getNotificationChannel(ShoppingReminderWorker.CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return@withLock 0
        val tag = "shopping:$scope"
        val enabled = snapshot.lists.filter { it.remindersEnabled }.map { it.id.hashCode() }.toSet()
        manager.activeNotifications.filter { it.tag == tag && it.id !in enabled }.forEach { manager.cancel(tag, it.id) }
        val prefs = context.getSharedPreferences("shopping_reminder_delivery", Context.MODE_PRIVATE)
        var sent = 0
        for (list in snapshot.lists.filter { it.remindersEnabled }) {
            val key = "$scope:${list.id}"
            val missing = ShoppingPlanning.pendingTomorrow(snapshot, list.id, now.plusDays(1).dayOfWeek.value)
            if (missing.isEmpty()) { manager.cancel(tag, list.id.hashCode()); continue }
            if (now.hour * 60 + now.minute < list.reminderTimeMinutes || prefs.getString(key, null) == now.toLocalDate().toString()) continue
            if (!isCurrent()) return@withLock sent
            val intent = Intent(context, MainActivity::class.java).apply {
                data = android.net.Uri.parse("dietreminder://shoppinglists")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val notification = NotificationCompat.Builder(context, ShoppingReminderWorker.CHANNEL)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Per domani · ${list.name}")
                .setContentText(missing.joinToString(", ").take(200))
                .setStyle(NotificationCompat.BigTextStyle().bigText("Ricorda di acquistare: ${missing.joinToString(", ").take(1500)}"))
                .setContentIntent(NotificationHelper.createActivityPendingIntent(context, key.hashCode(), intent))
                .setAutoCancel(true).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
            manager.notify(tag, list.id.hashCode(), notification)
            if (!isCurrent()) { manager.cancel(tag, list.id.hashCode()); return@withLock sent }
            prefs.edit(commit = true) { putString(key, now.toLocalDate().toString()) }
            sent++
        }
        sent
    }
}
