package it.matato.dietreminder.util.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.matato.dietreminder.util.AlarmSyncHelper
import it.matato.dietreminder.util.AppLog

class AlarmCheckerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppLog.d("AlarmCheckerWorker running...")

        return try {
            AlarmSyncHelper.doSync(applicationContext)
            Result.success()
        } catch (e: Exception) {
            AppLog.e("AlarmCheckerWorker failed", e)
            Result.retry()
        }
    }
}