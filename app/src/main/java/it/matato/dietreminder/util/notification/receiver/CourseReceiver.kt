package it.matato.dietreminder.util.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import it.matato.dietreminder.R
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.notification.NotificationChannelConfig
import it.matato.dietreminder.util.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CourseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                process(context, intent)
            } catch (e: Exception) {
                AppLog.e("CourseReceiver: Error processing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun process(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: context.getString(R.string.app_name)

        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?: context.getString(R.string.it_is_time_to_eat)

        val mealId = intent.getLongExtra(EXTRA_MEAL_ID, -1L)
        val dayName = intent.getStringExtra(EXTRA_DAY_NAME)

        AppLog.i("CourseReceiver triggered")

        val notificationIntent = Intent(
            Intent.ACTION_VIEW,
            createDeepLink(mealId, dayName)
        ).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val notificationId = createNotificationId(mealId, dayName)

        val contentIntent = NotificationHelper.createActivityPendingIntent(
            context = context,
            requestCode = notificationId,
            intent = notificationIntent
        )

        NotificationHelper(context).show(
            notificationId = notificationId,
            title = title,
            message = message,
            contentIntent = contentIntent,
            channel = NotificationChannelConfig(
                id = "corse_alarm",
                sound = NotificationChannelConfig.customSound(
                    context,
                    R.raw.meal_alarm,
                ),
            ),
        )

        AlarmSyncHelper.doSync(context)
    }

    private fun createDeepLink(
        mealId: Long,
        dayName: String?
    ) = if (mealId != -1L && !dayName.isNullOrBlank()) {
        "dietreminder://week?mealId=$mealId&dayName=$dayName".toUri()
    } else {
        "dietreminder://week".toUri()
    }

    private fun createNotificationId(
        mealId: Long,
        dayName: String?
    ): Int {
        return "$mealId-$dayName".hashCode()
    }

    private companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_MEAL_ID = "meal_id"
        const val EXTRA_DAY_NAME = "day_name"
    }
}