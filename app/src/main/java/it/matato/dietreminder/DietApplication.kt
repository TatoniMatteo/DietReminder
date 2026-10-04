package it.matato.dietreminder

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import it.matato.dietreminder.data.database.AppDatabase
import it.matato.dietreminder.data.repository.contracts.ConfigRepository
import it.matato.dietreminder.data.repository.contracts.DietRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.alarm.AlarmDataObserver
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.widget.WidgetRefreshWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
open class DietApplication : Application() {

	@Inject
	lateinit var database: AppDatabase

	@Inject
	lateinit var dietRepository: DietRepository

	@Inject
	lateinit var shoppingListRepository: ShoppingListRepository

	@Inject
	lateinit var configRepository: ConfigRepository

	override fun onCreate() {
		super.onCreate()
		initServices()
	}

	open fun initServices() {
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
