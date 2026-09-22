package it.matato.dietreminder.util.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.matato.dietreminder.MainActivity
import it.matato.dietreminder.R
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.notification.NotificationHelper

class TestAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AppLog.d("TestAlarmReceiver: test alarm received")

        val notificationHelper = NotificationHelper(context)

        val contentIntent = NotificationHelper.createActivityPendingIntent(
            context = context,
            requestCode = TEST_NOTIFICATION_ID,
            intent = Intent(context, MainActivity::class.java)
        )

        notificationHelper.show(
            notificationId = TEST_NOTIFICATION_ID,
            title = context.getString(R.string.test_scheduler),
            message = context.getString(R.string.test_scheduler_desc),
            contentIntent = contentIntent
        )
    }

    companion object {
        const val TEST_NOTIFICATION_ID = 10_001
    }
}