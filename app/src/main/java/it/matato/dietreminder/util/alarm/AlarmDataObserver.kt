package it.matato.dietreminder.util.alarm

import android.content.Context
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.util.AppLog
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

object AlarmDataObserver {

    private const val DEBOUCE = 10L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    fun start(context: Context) {
        val app = context.applicationContext as DietApplication

        scope.launch {
            createChangeFlow(app)
                .debounce(DEBOUCE.seconds)
                .collect { change ->
                    AppLog.i("AlarmDataObserver: $change detected")

                    try {
                        AlarmSyncHelper.doSync(app)
                    } catch (e: Exception) {
                        AppLog.e("AlarmDataObserver: failed to sync alarms", e)
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createChangeFlow(app: DietApplication): Flow<Change> {
        val activeDietChanges = app.repository.active
            .map { Change.ActiveDiet }

        val mealChanges = app.repository.active
            .flatMapLatest { activeDiet ->
                activeDiet?.let {
                    app.repository.observeMeals(it.id)
                        .map { Change.Meals }
                } ?: emptyFlow()
            }

        val configurationChanges = merge(
            observeConfigChanges(app, ConfigKey.MEAL_REMINDERS_ENABLED),
            observeConfigChanges(app, ConfigKey.HYDRATION_ENABLED),
            observeConfigChanges(app, ConfigKey.HYDRATION_RANGES),
            observeConfigChanges(app, ConfigKey.HYDRATION_DAYS),
            observeConfigChanges(app, ConfigKey.HYDRATION_INTERVAL),
        )

        return merge(
            activeDietChanges,
            mealChanges,
            configurationChanges,
        )
    }

    private fun observeConfigChanges(
        app: DietApplication,
        key: ConfigKey,
    ): Flow<Change> =
        app.repository.observeConfig(key)
            .distinctUntilChangedBy { it?.value }
            .map { Change.Config(key) }

    private sealed interface Change {
        data object ActiveDiet : Change
        data object Meals : Change
        data class Config(val key: ConfigKey) : Change
    }
}