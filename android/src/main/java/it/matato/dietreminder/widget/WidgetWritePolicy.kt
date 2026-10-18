package it.matato.dietreminder.widget

import android.content.Context
import it.matato.dietreminder.data.repository.remote.ServerVersionPolicyRepository
import it.matato.dietreminder.util.UpdateManager
import it.matato.dietreminder.util.UpdateManagerStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Widgets can start a fresh process without ever opening the activity/view model. */
internal object WidgetWritePolicy {
    private val lock = Mutex()
    suspend fun ensureInitialized(context: Context, repository: it.matato.dietreminder.data.repository.contracts.VersionPolicyRepository = ServerVersionPolicyRepository(context)) = lock.withLock {
        if (UpdateManagerStatus.isChecking) UpdateManager(repository).checkUpdate()
    }
}
