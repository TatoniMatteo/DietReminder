package it.matato.dietreminder.util.shopping

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import dagger.hilt.android.EntryPointAccessors
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.data.sync.CloudSyncEntryPoint
import it.matato.dietreminder.data.sync.WorkspaceCache
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import it.matato.dietreminder.data.database.entity.ConfigKey
import kotlinx.coroutines.CancellationException

/** Persistent periodic work survives process death and reboot; no exact-alarm permission. */
class ShoppingReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? DietApplication ?: return Result.success()
        val account = app.sessions.state.value
        val scope = account.scope
        if (scope == "locked") return Result.success()
        return try {
            // Read current server purchases before notifying a cloud account.
            if (scope != "local") {
                val deps = EntryPointAccessors.fromApplication(app, CloudSyncEntryPoint::class.java)
                if (!deps.manager().sync(scope)) return Result.retry()
            }
            if (app.currentAccountScope() != scope) return Result.success()
            val repositories = app.accountRepositories.forScope(scope)
            if (repositories.journal?.state()?.conflictVersion != null) return Result.retry()
            val db = repositories.database ?: return Result.success()
            val snapshot = db.withTransaction { WorkspaceCache(db).read() }
            val reminderTime = repositories.config.observeConfig(ConfigKey.SHOPPING_REMINDER_TIME).first()
                ?.value?.toIntOrNull()?.takeIf { it in 0..1439 } ?: 1080
            ShoppingReminderNotifier.deliver(app, snapshot, scope, LocalDateTime.now(), reminderTime) { app.currentAccountScope() == scope }
            Result.success()
        } catch (ex: CancellationException) { throw ex }
        catch (_: Exception) { Result.retry() }
    }
    companion object {
        const val CHANNEL = "shopping_reminders"
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("shopping-reminders", ExistingPeriodicWorkPolicy.UPDATE, PeriodicWorkRequestBuilder<ShoppingReminderWorker>(15, TimeUnit.MINUTES).build())
        }
    }
}
