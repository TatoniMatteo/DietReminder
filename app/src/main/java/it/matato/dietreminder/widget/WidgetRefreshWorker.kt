package it.matato.dietreminder.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        DietReminder.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "diet-widget-refresh"
    }
}
