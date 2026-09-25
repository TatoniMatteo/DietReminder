package it.matato.dietreminder.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.entity.Meal
import it.matato.dietreminder.data.database.relation.CourseWithItems
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.repository.DietRepository
import it.matato.dietreminder.domain.NextMeal
import it.matato.dietreminder.domain.nextMeal
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.alarm.AlarmScheduler
import it.matato.dietreminder.util.alarm.AlarmSyncHelper
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

sealed interface ImportCheckResult {
    data object Valid : ImportCheckResult
    data object Conflict : ImportCheckResult
    data class Invalid(val message: String) : ImportCheckResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class DietViewModel(
    private val application: Application,
    private val repository: DietRepository
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                val repository = if (app is DietApplication) {
                    app.repository
                } else {
                    // Fallback per l'ambiente di test (es. TestDietApplication)
                    val repoField = app?.javaClass?.getMethod("getRepository")?.invoke(app) as? DietRepository
                        ?: (app?.javaClass?.getField("repository")?.get(app) as DietRepository)
                    repoField
                }
                return DietViewModel(app, repository) as T
            }
        }
    }

    val diets = repository.all.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val active = repository.active.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null,
    )

    val meals = active
        .flatMapLatest { diet ->
            diet?.let { repository.observeMeals(it.id) } ?: flowOf(emptyList())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val defaultTimes = repository.defaultTimes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val theme = repository.observeConfig(ConfigKey.THEME)
        .map { it?.value ?: "system" }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "system",
        )

    val useDynamicColors = repository.observeConfig(ConfigKey.USE_DYNAMIC_COLORS)
        .map { it?.value != "false" }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true,
        )

    val seedColor = repository.observeConfig(ConfigKey.SEED_COLOR)
        .map { it?.value ?: "0xFF6750A4" }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "0xFF6750A4",
        )

    val language = repository.observeConfig(ConfigKey.LANGUAGE)
        .map { it?.value ?: "it" }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "it",
        )

    val hydrationRanges = repository.observeConfig(ConfigKey.HYDRATION_RANGES)
        .map { config ->
            config?.value?.let { value ->
                try {
                    Json.decodeFromString<List<HydrationRange>>(value)
                } catch (_: Exception) {
                    defaultHydrationRanges()
                }
            } ?: defaultHydrationRanges()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            defaultHydrationRanges(),
        )

    val hydrationEnabled = repository.observeConfig(ConfigKey.HYDRATION_ENABLED)
        .map { it?.value == "true" }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true,
        )

    val hydrationInterval = repository.observeConfig(ConfigKey.HYDRATION_INTERVAL)
        .map { it?.value?.toIntOrNull() ?: 120 }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            120,
        )

    val hydrationDays = repository.observeConfig(ConfigKey.HYDRATION_DAYS)
        .map { config ->
            config?.value
                ?.split(",")
                ?.mapNotNull { value ->
                    runCatching { DayOfWeek.valueOf(value) }.getOrNull()
                }
                ?.toSet()
                ?: DayOfWeek.entries.toSet()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DayOfWeek.entries.toSet(),
        )

    val mealRemindersEnabled = repository.observeConfig(ConfigKey.MEAL_REMINDERS_ENABLED)
        .map { it?.value != "false" }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true,
        )

    val isDeveloperMode = repository.observeConfig(ConfigKey.DEVELOPER_MODE)
        .map { it?.value == "true" }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false,
        )

    val appLogs = AppLog.entries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    val lastImportError = mutableStateOf<String?>(null)

    fun setTheme(value: String) {
        viewModelScope.launch {
            repository.saveConfig(ConfigKey.THEME, value)
        }
    }

    fun setUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveConfig(ConfigKey.USE_DYNAMIC_COLORS, enabled.toString())
        }
    }

    fun setSeedColor(value: String) {
        viewModelScope.launch {
            repository.saveConfig(ConfigKey.SEED_COLOR, value)
        }
    }

    fun setLanguage(value: String) {
        viewModelScope.launch {
            repository.saveConfig(ConfigKey.LANGUAGE, value)
        }
    }

    fun setHydrationRanges(ranges: List<HydrationRange>) {
        viewModelScope.launch {
            repository.saveConfig(
                ConfigKey.HYDRATION_RANGES,
                Json.encodeToString(ranges),
            )
        }
    }

    fun setHydrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveConfig(
                ConfigKey.HYDRATION_ENABLED,
                enabled.toString(),
            )
        }
    }

    fun setHydrationInterval(minutes: Int) {
        viewModelScope.launch {
            repository.saveConfig(
                ConfigKey.HYDRATION_INTERVAL,
                minutes.toString(),
            )
        }
    }

    fun setHydrationDays(days: Set<DayOfWeek>) {
        viewModelScope.launch {
            repository.saveConfig(
                ConfigKey.HYDRATION_DAYS,
                days.joinToString(",") { it.name },
            )
        }
    }

    fun setMealRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveConfig(
                ConfigKey.MEAL_REMINDERS_ENABLED,
                enabled.toString(),
            )
            AlarmSyncHelper.syncAlarms(application)
        }
    }

    fun setDietDayNotificationEnabled(dietId: Long, day: DayOfWeek, enabled: Boolean) {
        viewModelScope.launch {
            val diet = diets.value.find { it.id == dietId } ?: return@launch
            val updated = diet.withDayNotificationToggled(day, enabled)
            repository.updateDiet(updated)
            DietReminder.updateAll(application)
            AlarmSyncHelper.syncAlarms(application)
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveConfig(
                ConfigKey.DEVELOPER_MODE,
                enabled.toString(),
            )
        }
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
        return defaultTimes.value
            .find { it.type == type }
            ?.timeMinutes
            ?: getDefaultFallback(type)
    }

    suspend fun getDietMeals(id: Long) = repository.getMeals(id)

    suspend fun getMeal(id: Long): MealWithDetails? {
        if (id == 0L) {
            return null
        }

        return repository
            .getMeals(active.value?.id ?: 0L)
            .find { it.meal.id == id }
    }

    fun next(): NextMeal? {
        return active.value?.let {
            nextMeal(
                meals.value,
                LocalDateTime.now(),
                it.nextMealWindowMinutes,
            )
        }
    }

    fun scheduleTestAlarm(seconds: Int) {
        AppLog.d("Scheduling test alarm in $seconds seconds...")

        AlarmScheduler.scheduleTestAlarm(
            context = application,
            delayMillis = seconds * 1000L
        )
    }

    fun saveDefaultTime(type: MealType, timeMinutes: Int) {
        viewModelScope.launch {
            repository.saveDefaultTime(type, timeMinutes)
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            repository.resetDatabase()
        }
    }

    fun create(name: String, window: Int) {
        viewModelScope.launch {
            repository.create(name, window)
        }
    }

    fun activate(id: Long) {
        viewModelScope.launch {
            repository.activate(id)
            DietReminder.updateAll(application)
        }
    }

    fun deleteDiet(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            DietReminder.updateAll(application)
        }
    }

    fun duplicate(id: Long) {
        viewModelScope.launch {
            repository.duplicate(id)
        }
    }

    fun saveMeal(meal: Meal, courses: List<CourseWithItems>) {
        viewModelScope.launch {
            repository.saveMeal(meal, courses)
            DietReminder.updateAll(application)
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
            DietReminder.updateAll(application)
        }
    }

    suspend fun checkImportConflict(json: String): ImportCheckResult {
        return try {
            lastImportError.value = null

            val data = repository.parseDietJson(json)

            val existing = data.uuid?.let { repository.dietExists(it) }

            if (existing == true) {
                ImportCheckResult.Conflict
            } else {
                ImportCheckResult.Valid
            }
        } catch (exception: Exception) {
            AppLog.e("Import validation failed", exception)

            val message = exception.localizedMessage ?: "Invalid import data"
            lastImportError.value = message

            ImportCheckResult.Invalid(message)
        }
    }

    suspend fun importDiet(
        json: String,
        overwrite: Boolean = false,
    ): Result<Unit> {
        return try {
            lastImportError.value = null
            repository.importJson(json, overwrite)
            Result.success(Unit)
        } catch (exception: Exception) {
            AppLog.e("Import failed", exception)

            val message = exception.localizedMessage ?: "Unknown error"
            lastImportError.value = message

            Result.failure(exception)
        }
    }

    suspend fun exportDiet(id: Long): String {
        return repository.exportJson(id)
    }

    private fun defaultHydrationRanges(): List<HydrationRange> {
        return listOf(
            HydrationRange(
                startMinutes = 8 * 60,
                endMinutes = 22 * 60,
            ),
        )
    }
}
