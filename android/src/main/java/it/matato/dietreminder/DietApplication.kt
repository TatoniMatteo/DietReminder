package it.matato.dietreminder

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import it.matato.dietreminder.data.repository.contracts.ConfigRepository
import it.matato.dietreminder.data.repository.contracts.DietRepository
import it.matato.dietreminder.data.repository.contracts.ShoppingListRepository
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.alarm.AlarmDataObserver
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
import it.matato.dietreminder.widget.WidgetRefreshWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@HiltAndroidApp
open class DietApplication : Application() {

    @Inject
    lateinit var dietRepository: DietRepository

    @Inject
    lateinit var shoppingListRepository: ShoppingListRepository

    @Inject
    lateinit var configRepository: ConfigRepository

    @Inject
    lateinit var sessions: it.matato.dietreminder.data.auth.AccountSessionStore

    @Inject
    lateinit var cloudSyncScheduler: it.matato.dietreminder.data.sync.CloudSyncScheduler
    @Inject
    lateinit var accountRepositories: it.matato.dietreminder.data.auth.AccountRepositories
    private val accountScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun currentAccountScope(): String = if (::sessions.isInitialized) sessions.state.value.scope else "local"

    override fun onCreate() {
        super.onCreate()
        initServices()
    }

    open fun initServices() {
        AppLog.i("=== Application Initializing ===")

        accountScope.launch {
            sessions.state.map { it.scope }.distinctUntilChanged().collect { scope ->
                cloudSyncScheduler.enqueue(scope)
                getSystemService(android.app.NotificationManager::class.java).cancelAll()
                try {
                    AlarmSyncHelper.doSync(this@DietApplication)
                } finally {
                    it.matato.dietreminder.widget.Widgets.updateAll(this@DietApplication)
                }
            }
        }

        it.matato.dietreminder.widget.Widgets.observe(this, accountScope)

        // Sync alarms at startup
        AlarmSyncHelper.syncAlarms(this)
        AlarmDataObserver.start(this)

        it.matato.dietreminder.util.shopping.ShoppingReminderWorker.schedule(this)
        cloudSyncScheduler.startPeriodic()
        AppLog.d("Setting up Periodic Workers")

        val widgetRequest = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(WidgetRefreshWorker.WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, widgetRequest)

        AppLog.i("=== Application Ready ===")
    }
}
