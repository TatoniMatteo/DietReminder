package it.matato.dietreminder.util.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import it.matato.dietreminder.R
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.util.notification.NotificationChannelConfig
import it.matato.dietreminder.util.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HydrationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                process(context)
            } catch (e: Exception) {
                AppLog.e("HydrationReceiver: Error processing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun process(context: Context) {
        AppLog.i("HydrationReceiver triggered")

        val notificationIntent = Intent(
            Intent.ACTION_VIEW,
            "dietreminder://hydration".toUri(),
        ).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val notificationId = HYDRATION_NOTIFICATION_ID

        val contentIntent = NotificationHelper.createActivityPendingIntent(
            context = context,
            requestCode = notificationId,
            intent = notificationIntent,
        )

        NotificationHelper(context).show(
            notificationId = notificationId,
            title = context.getString(R.string.hydration_notification_title),
            message = context.getString(R.string.hydration_notification_message),
            contentIntent = contentIntent,
            channel = NotificationChannelConfig(
                id = "hydration_alarm",
                sound = NotificationChannelConfig.customSound(
                    context,
                    R.raw.hydration_alarm,
                ),
            ),
        )

        AlarmSyncHelper.doSync(context)
    }

    private companion object {
        const val HYDRATION_NOTIFICATION_ID = 999
    }
}
