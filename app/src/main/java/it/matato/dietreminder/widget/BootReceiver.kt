package it.matato.dietreminder.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.util.AppLog
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLog.d("BootReceiver received action: ${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmSyncHelper.syncAlarms(context)

            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WidgetRefreshWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
