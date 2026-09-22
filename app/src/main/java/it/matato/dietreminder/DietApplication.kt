package it.matato.dietreminder

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.repository.DietRepository
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.alarm.AlarmDataObserver
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.widget.WidgetRefreshWorker
import java.util.concurrent.TimeUnit

class DietApplication : Application() {

    val database by lazy {
        AppDatabase.create(this)
    }

    val repository by lazy {
        DietRepository(
            database = database,
            diets = database.dietDao(),
            meals = database.mealDao(),
            courses = database.courseDao(),
            foodItems = database.foodItemDao(),
            config = database.configDao(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.i("=== Application Initializing ===")

        // Sync alarms at startup
        AlarmSyncHelper.syncAlarms(this)
        AlarmDataObserver.start(this)

        AppLog.d("Setting up Periodic Workers")

        val widgetRequest = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(WidgetRefreshWorker.WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, widgetRequest)

        AppLog.i("=== Application Ready ===")
    }
}
