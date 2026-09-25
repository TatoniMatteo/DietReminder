package it.matato.dietreminder.util.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import it.matato.dietreminder.R

class NotificationHelper(
    private val context: Context,
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
    private val channelManager: NotificationChannelManager =
        NotificationChannelManager(context, notificationManager),
) {

    fun show(
        notificationId: Int,
        title: String,
        message: String,
        contentIntent: PendingIntent,
        channel: NotificationChannelConfig = NotificationChannelConfig(),
    ) {
        channelManager.ensureChannel(channel)

        notificationManager.notify(
            notificationId,
            NotificationCompat.Builder(context, channel.id)
                .setSmallIcon(R.drawable.app_icon_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        fun createActivityPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent,
        ): PendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
