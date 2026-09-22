package it.matato.dietreminder.util.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AppLog.i("AlarmSyncReceiver triggered")
                AlarmSyncHelper.doSync(context)
            } catch (e: Exception) {
                AppLog.e("AlarmSyncReceiver: Error synchronizing alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}