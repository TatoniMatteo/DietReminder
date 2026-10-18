package it.matato.dietreminder.widget

import android.content.Context
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.DietApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object Widgets {
    suspend fun updateAll(context: Context) {
        DietReminder.updateAll(context)
        ShoppingWidget.updateAll(context)
    }
    /** Observe the pinned account stores so cloud changes, shopping ticks and themes reach the home screen. */
    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    fun observe(app: DietApplication, scope: CoroutineScope) {
        scope.launch {
            app.accountRepositories.changes.flatMapLatest { repos ->
                merge(
                    repos.diets.active.flatMapLatest { diet -> if (diet == null) flowOf(Unit) else repos.diets.observeMeals(diet.id).map { Unit } },
                    repos.shopping.allShoppingLists.map { Unit },
                    merge(*it.matato.dietreminder.data.database.entity.ConfigKey.entries.filter { it.name in setOf("THEME", "USE_DYNAMIC_COLORS", "SEED_COLOR", "LANGUAGE") }.map { repos.config.observeConfig(it).map { Unit } }.toTypedArray()),
                )
            }.debounce(500).collect { runCatching { updateAll(app) }.onFailure { error -> AppLog.e("Widgets: refresh failed", error) } }
        }
    }
}
