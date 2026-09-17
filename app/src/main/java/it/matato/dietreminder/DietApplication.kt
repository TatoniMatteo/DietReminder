package it.matato.dietreminder

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import it.matato.dietreminder.data.AppDatabase
import it.matato.dietreminder.data.DietRepository
import it.matato.dietreminder.util.AlarmSyncHelper
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.worker.AlarmCheckerWorker
import it.matato.dietreminder.widget.WidgetRefreshWorker
import java.util.concurrent.TimeUnit

class DietApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy {
        DietRepository(
            database.dietDao(),
            database.mealDao(),
            database.courseDao(),
            database.foodItemDao(),
            database.configDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.i("=== Application Initializing ===")
        
        // Sync alarms at startup
        AlarmSyncHelper.syncAlarms(this)

        AppLog.d("Setting up Periodic Workers")
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        val checkerRequest = PeriodicWorkRequestBuilder<AlarmCheckerWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AlarmChecker",
            ExistingPeriodicWorkPolicy.KEEP,
            checkerRequest
        )
        AppLog.i("=== Application Ready ===")
    }
}
