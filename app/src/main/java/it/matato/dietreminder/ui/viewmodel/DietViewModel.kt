package it.matato.dietreminder.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.R
import it.matato.dietreminder.data.CourseWithItems
import it.matato.dietreminder.data.HydrationRange
import it.matato.dietreminder.data.MealEntity
import it.matato.dietreminder.data.MealType
import it.matato.dietreminder.data.MealWithDetails
import it.matato.dietreminder.domain.NextMeal
import it.matato.dietreminder.domain.nextMeal
import it.matato.dietreminder.util.AlarmReceiver
import it.matato.dietreminder.util.AlarmScheduler
import it.matato.dietreminder.util.AlarmSyncHelper
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.widget.DietReminder
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class DietViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as DietApplication).repository

    val diets = repo.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val active = repo.active.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val meals = active.flatMapLatest { diet ->
        diet?.let { repo.observeMeals(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getDietMeals(id: Long) = repo.getMeals(id)

    val defaultTimes = repo.defaultTimes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val theme = repo.observeConfig("theme").map { it?.value ?: "system" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun setTheme(value: String) = viewModelScope.launch { repo.saveConfig("theme", value) }

    val useDynamicColors = repo.observeConfig("use_dynamic_colors").map { it?.value != "false" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setUseDynamicColors(enabled: Boolean) = viewModelScope.launch {
        repo.saveConfig("use_dynamic_colors", enabled.toString())
    }

    fun getDefaultFallback(type: MealType): Int {
        return when (type) {
            MealType.BREAKFAST -> 8 * 60
            MealType.MORNING_SNACK -> 10 * 60 + 30
            MealType.LUNCH -> 13 * 60
            MealType.AFTERNOON_SNACK -> 16 * 60 + 30
            MealType.DINNER -> 20 * 60
            MealType.OTHER -> 12 * 60
        }
    }

    fun getDefaultTime(type: MealType): Int {
        return defaultTimes.value.find { it.type == type }?.timeMinutes ?: getDefaultFallback(type)
    }

    val seedColor = repo.observeConfig("seed_color").map { it?.value ?: "0xFF6750A4" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0xFF6750A4")

    fun setSeedColor(value: String) = viewModelScope.launch { repo.saveConfig("seed_color", value) }

    val language = repo.observeConfig("language").map { it?.value ?: "it" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "it")

    fun setLanguage(value: String) = viewModelScope.launch { repo.saveConfig("language", value) }

    val hydrationRanges = repo.observeConfig("hydration_ranges").map { config ->
        config?.value?.let {
            try {
                Json.decodeFromString<List<HydrationRange>>(it)
            } catch (_: Exception) {
                listOf(HydrationRange(8 * 60, 22 * 60))
            }
        } ?: listOf(HydrationRange(8 * 60, 22 * 60))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(HydrationRange(8 * 60, 22 * 60)))

    fun setHydrationRanges(ranges: List<HydrationRange>) = viewModelScope.launch {
        val json = Json.encodeToString(ranges)
        repo.saveConfig("hydration_ranges", json)
        syncAlarms()
    }

    val hydrationEnabled = repo.observeConfig("hydration_enabled").map { it?.value == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hydrationInterval = repo.observeConfig("hydration_interval").map { it?.value?.toIntOrNull() ?: 120 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120)

    val hydrationDays = repo.observeConfig("hydration_days").map { config ->
        config?.value?.split(",")?.mapNotNull { 
            try { DayOfWeek.valueOf(it) } catch(_: Exception) { null }
        }?.toSet() ?: DayOfWeek.entries.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayOfWeek.entries.toSet())

    fun setHydrationEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.saveConfig("hydration_enabled", enabled.toString())
        syncAlarms()
    }

    fun setHydrationInterval(minutes: Int) = viewModelScope.launch {
        repo.saveConfig("hydration_interval", minutes.toString())
        syncAlarms()
    }

    fun setHydrationDays(days: Set<DayOfWeek>) = viewModelScope.launch {
        repo.saveConfig("hydration_days", days.joinToString(",") { it.name })
        syncAlarms()
    }

    val mealRemindersEnabled = repo.observeConfig("meal_reminders_enabled").map { it?.value != "false" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setMealRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.saveConfig("meal_reminders_enabled", enabled.toString())
        syncAlarms()
    }

    val isDeveloperMode = repo.observeConfig("developer_mode").map { it?.value == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDeveloperMode(enabled: Boolean) = viewModelScope.launch {
        repo.saveConfig("developer_mode", enabled.toString())
    }

    val appLogs = AppLog.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun triggerTestAlarm() {
        AppLog.d("Triggering immediate test alarm...")
        val context = getApplication<Application>()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", context.getString(R.string.immediate_trigger))
            putExtra("message", context.getString(R.string.immediate_trigger_desc))
            putExtra("type", "TEST")
        }
        context.sendBroadcast(intent)
    }

    fun scheduleTestAlarm(seconds: Int) {
        AppLog.d("Scheduling test alarm in $seconds seconds...")
        val context = getApplication<Application>()
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        
        AlarmScheduler.scheduleExactAlarm(
            context,
            MealType.OTHER,
            triggerAt,
            context.getString(R.string.test_scheduler),
            context.getString(R.string.test_scheduler_desc),
            mapOf("type" to "TEST")
        )
    }

    fun saveDefaultTime(type: MealType, timeMinutes: Int) = viewModelScope.launch {
        repo.saveDefaultTime(type, timeMinutes)
        syncAlarms()
    }

    fun syncAlarms() {
        AlarmSyncHelper.syncAlarms(getApplication())
    }

    fun resetDatabase() = viewModelScope.launch {
        repo.resetDatabase()
        syncAlarms()
    }

    suspend fun getMeal(id: Long): MealWithDetails? {
        if (id == 0L) return null
        return repo.getMeals(active.value?.id ?: 0L).find { it.meal.id == id }
    }

    fun next(): NextMeal? = active.value?.let { nextMeal(meals.value, LocalDateTime.now(), it.nextMealWindowMinutes) }

    fun create(name: String, window: Int) = viewModelScope.launch { repo.create(name, window) }

    fun activate(id: Long) = viewModelScope.launch {
        repo.activate(id)
        DietReminder.updateAll(getApplication())
    }

    fun deleteDiet(id: Long) = viewModelScope.launch {
        repo.delete(id)
        DietReminder.updateAll(getApplication())
    }

    fun duplicate(id: Long) = viewModelScope.launch { repo.duplicate(id) }

    fun saveMeal(meal: MealEntity, courses: List<CourseWithItems>) = viewModelScope.launch {
        repo.saveMeal(meal, courses)
        DietReminder.updateAll(getApplication())
    }

    fun deleteMeal(id: Long) = viewModelScope.launch {
        repo.deleteMeal(id)
        DietReminder.updateAll(getApplication())
    }

    val lastImportError = mutableStateOf<String?>(null)

    fun importDiet(json: String, overwrite: Boolean = false) = viewModelScope.launch {
        try {
            lastImportError.value = null
            repo.importJson(json, overwrite)
        } catch (e: Exception) {
            AppLog.e("Import failed", e)
            lastImportError.value = e.localizedMessage ?: "Unknown error"
        }
    }

    suspend fun checkImportConflict(json: String): Boolean {
        return try {
            lastImportError.value = null
            val data = repo.parseDietJson(json)
            data.uuid?.let { repo.dietExists(it) } ?: false
        } catch (e: Exception) {
            lastImportError.value = e.localizedMessage ?: "Parsing failed"
            false
        }
    }

    suspend fun exportDiet(id: Long): String = repo.exportJson(id)
}
