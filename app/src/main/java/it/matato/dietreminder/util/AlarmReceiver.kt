package it.matato.dietreminder.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.HydrationRange
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                processAlarm(context, intent)
            } catch (e: Exception) {
                AppLog.e("AlarmReceiver: Error processing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processAlarm(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val message = intent.getStringExtra(EXTRA_MESSAGE)

        AppLog.i("AlarmReceiver triggered! Action: ${intent.action}, Type: $type")
        AppLog.d("Payload: $title - $message")

        if (type == TYPE_DAILY_SYNC) {
            AppLog.i("Daily alarm sync triggered")
            AlarmSyncHelper.doSync(context)
            return
        }

        if (type == TYPE_HYDRATION && !isHydrationAlarmValid(intent)) {
            AppLog.i("Hydration alarm fired outside allowed windows, suppressing notification")
            AlarmSyncHelper.doSync(context)
            return
        }

        val notificationTitle = title ?: context.getString(R.string.app_name)
        val notificationMessage = message ?: context.getString(R.string.it_is_time_to_eat)

        if (isMealAlarm(type)) {
            AlarmSoundPlayer.playMealAlarm(context)
            AppLog.i("Meal alarm sound started for type=$type")
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(
            context = context,
            notificationManager = notificationManager,
            channelName = context.getString(R.string.notifications_alarms)
        )

        val notificationId = System.currentTimeMillis().toInt()
        val activityIntent = Intent(
            Intent.ACTION_VIEW, createDeepLink(intent, type)
        ).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context, notificationId, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.app_icon_foreground)
            .setContentTitle(notificationTitle).setContentText(notificationMessage).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM).setContentIntent(contentPendingIntent).setAutoCancel(true)

        if (type == TYPE_HYDRATION) {
            val laterIntent = Intent(context, HydrationReceiver::class.java).apply {
                action = ACTION_LATER
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }

            val laterPendingIntent = PendingIntent.getBroadcast(
                context, notificationId + 1, laterIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                0, context.getString(R.string.action_later), laterPendingIntent
            )
        }

        try {
            notificationManager.notify(notificationId, builder.build())
            AppLog.i("Notification posted: ID=$notificationId, Type=$type")
        } catch (e: Exception) {
            AppLog.e("System failed to post notification", e)
        }

        AppLog.t("Synchronizing alarms after handling current alarm")
        AlarmSyncHelper.doSync(context)
    }

    private fun isMealAlarm(type: String?): Boolean {
        return !type.isNullOrBlank() && type != TYPE_DAILY_SYNC && type != TYPE_HYDRATION
    }

    private fun isHydrationAlarmValid(intent: Intent): Boolean {
        val rangesJson = intent.getStringExtra(EXTRA_RANGES_JSON)

        if (rangesJson.isNullOrBlank()) {
            AppLog.w("Hydration alarm has no ranges information")
            return false
        }

        val ranges = try {
            Json.decodeFromString<List<HydrationRange>>(rangesJson)
        } catch (e: Exception) {
            AppLog.e("Failed to decode hydration ranges from intent", e)
            return false
        }

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val isInRange = ranges.any { range ->
            when {
                range.startMinutes < range.endMinutes -> currentMinutes >= range.startMinutes && currentMinutes < range.endMinutes
                range.endMinutes == 0 -> currentMinutes >= range.startMinutes
                range.startMinutes > range.endMinutes -> currentMinutes >= range.startMinutes || currentMinutes < range.endMinutes
                else -> false
            }
        }
        AppLog.d("Hydration window check: time=$currentMinutes, valid=$isInRange")

        return isInRange
    }

    private fun createNotificationChannel(context: Context, notificationManager: NotificationManager, channelName: String) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        AppLog.t("Creating silent notification channel: $CHANNEL_ID")

        val channel = NotificationChannel(
            CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.meal_reminders_desc)
            enableLights(true)
            enableVibration(false)
            setSound(null, null)
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun createDeepLink(intent: Intent, type: String?) =
        if (type == TYPE_HYDRATION) {
            "dietreminder://hydration".toUri()
        } else {
            val mealId = intent.getLongExtra(EXTRA_MEAL_ID, -1L)
            val dayName = intent.getStringExtra(EXTRA_DAY_NAME)

            if (mealId != -1L && !dayName.isNullOrBlank()) {
                "dietreminder://week?mealId=$mealId&dayName=$dayName".toUri()
            } else {
                "dietreminder://week".toUri()
            }
        }

    private companion object {
        const val CHANNEL_ID = "diet_alarms"

        const val TYPE_DAILY_SYNC = "DAILY_SYNC"
        const val TYPE_HYDRATION = "HYDRATION"

        const val EXTRA_TYPE = "type"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_RANGES_JSON = "ranges_json"
        const val EXTRA_MEAL_ID = "meal_id"
        const val EXTRA_DAY_NAME = "day_name"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        const val ACTION_LATER = "it.matato.dietreminder.ACTION_LATER"
    }
}
