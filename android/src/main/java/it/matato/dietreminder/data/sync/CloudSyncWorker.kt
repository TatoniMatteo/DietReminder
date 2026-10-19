package it.matato.dietreminder.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import it.matato.dietreminder.data.auth.AccountSessionStore
import kotlinx.coroutines.CancellationException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CloudSyncEntryPoint {
    fun manager(): CloudSyncManager
    fun sessions(): AccountSessionStore
}

class CloudSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, CloudSyncEntryPoint::class.java)
        val scope = inputData.getString("scope") ?: dependencies.sessions().state.value.scope
        return try {
            if (dependencies.manager().sync(scope)) Result.success() else Result.retry()
        } catch (ex: CancellationException) {
            throw ex
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
