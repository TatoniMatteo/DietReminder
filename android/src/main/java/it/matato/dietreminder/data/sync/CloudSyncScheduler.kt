package it.matato.dietreminder.data.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun enqueue(scope: String, replace: Boolean = false) {
        if (scope == "local" || scope == "locked") return
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInputData(workDataOf("scope" to scope))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(1, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "cloud-sync:$scope", if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, request
        )
    }
    fun startPeriodic() {
        val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("cloud-sync-periodic", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
