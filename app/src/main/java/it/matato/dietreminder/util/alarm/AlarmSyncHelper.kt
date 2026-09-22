package it.matato.dietreminder.util.alarm

import android.content.Context
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.util.AppLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

object AlarmSyncHelper {

    private const val DEFAULT_HYDRATION_INTERVAL_MINUTES = 120
    private val defaultHydrationRanges = listOf(
        HydrationRange(
            startMinutes = 8 * 60,
            endMinutes = 22 * 60
        )
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    fun syncAlarms(context: Context) {
        val applicationContext = context.applicationContext

        scope.launch {
            try {
                doSync(applicationContext)
            } catch (e: Exception) {
                AppLog.e("Failed to sync alarms", e)
            }
        }
    }

    suspend fun doSync(context: Context) {
        syncMutex.withLock {
            syncInternal(context.applicationContext)
        }
    }

    private suspend fun syncInternal(context: Context) {
        AppLog.i("AlarmSyncHelper: Sync starting...")

        val app = context as DietApplication
        val repository = app.repository

        val mealRemindersEnabled =
            repository.observeConfig(ConfigKey.MEAL_REMINDERS_ENABLED).first()?.value != "false"

        val hydrationEnabled =
            repository.observeConfig(ConfigKey.HYDRATION_ENABLED).first()?.value == "true"

        AppLog.d(
            "Current configuration: mealReminders=$mealRemindersEnabled, " +
                    "hydration=$hydrationEnabled"
        )

        AlarmScheduler.cancelAllAlarms(context)

        if (mealRemindersEnabled) {
            syncMealAlarms(context, app)
        }

        if (hydrationEnabled) {
            syncHydrationAlarm(context, app)
        }

        AlarmScheduler.scheduleDailySync(context)

        AppLog.i("AlarmSyncHelper: Sync completed successfully")
    }

    private suspend fun syncMealAlarms(
        context: Context,
        app: DietApplication
    ) {
        val activeDiet = app.repository.active.first()

        if (activeDiet == null) {
            AppLog.w("No active diet found, meal reminders will not be scheduled")
            return
        }

        AppLog.d("Syncing meals for active diet: ${activeDiet.name}")

        val allMeals = app.repository.getMeals(activeDiet.id)

        MealType.entries.forEach { type ->
            val matchingMeals = allMeals.filter { it.meal.type == type }
            val occurrence = findTodayMealOccurrence(matchingMeals)

            if (occurrence == null) {
                AppLog.d("No remaining ${type.name} meal for today")
                return@forEach
            }

            val meal = matchingMeals.firstOrNull {
                it.meal.dayOfWeek == occurrence.dayOfWeek &&
                        it.meal.timeMinutes == occurrence.hour * 60 + occurrence.minute
            }

            if (meal == null) {
                AppLog.w("Unable to find meal ID for ${type.name} at $occurrence")
                return@forEach
            }

            val timeMillis = occurrence
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val mealName = context.getString(type.resId)

            AlarmScheduler.scheduleMealAlarm(
                context = context,
                type = type,
                timeMillis = timeMillis,
                title = context.getString(R.string.it_is_time_to_eat),
                message = context.getString(R.string.it_is_moment_of, mealName),
                mealId = meal.meal.id,
                dayName = occurrence.dayOfWeek.name
            )
        }
    }

    private suspend fun syncHydrationAlarm(
        context: Context,
        app: DietApplication
    ) {
        val hydrationRanges = loadHydrationRanges(app)
        val activeDays = loadHydrationDays(app)
        val interval = loadHydrationInterval(app)
        val today = LocalDate.now().dayOfWeek

        if (today !in activeDays) {
            AppLog.i("Hydration reminder skipped: today ($today) is not an active day")
            return
        }

        if (interval <= 0) {
            AppLog.w("Hydration reminder skipped: invalid interval $interval")
            return
        }

        AppLog.t(
            "Scheduling hydration for today ($today) with interval $interval min"
        )

        AlarmScheduler.scheduleHydrationAlarm(
            context = context,
            intervalMinutes = interval,
            ranges = hydrationRanges
        )
    }

    private suspend fun loadHydrationRanges(app: DietApplication): List<HydrationRange> {
        val json = app.repository.observeConfig(ConfigKey.HYDRATION_RANGES).first()?.value

        if (json.isNullOrBlank()) {
            return defaultHydrationRanges
        }

        return try {
            Json.decodeFromString<List<HydrationRange>>(json)
        } catch (e: Exception) {
            AppLog.e("Failed to decode hydration ranges", e)
            defaultHydrationRanges
        }
    }

    private suspend fun loadHydrationDays(app: DietApplication): Set<DayOfWeek> {
        val value = app.repository.observeConfig(ConfigKey.HYDRATION_DAYS).first()?.value

        if (value.isNullOrBlank()) {
            return DayOfWeek.entries.toSet()
        }

        return value
            .split(",")
            .mapNotNull { day ->
                try {
                    DayOfWeek.valueOf(day.trim())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            .toSet()
            .ifEmpty {
                DayOfWeek.entries.toSet()
            }
    }

    private suspend fun loadHydrationInterval(app: DietApplication): Int {
        return app.repository.observeConfig(ConfigKey.HYDRATION_INTERVAL).first()?.value?.toIntOrNull()
            ?: DEFAULT_HYDRATION_INTERVAL_MINUTES
    }

    private fun findTodayMealOccurrence(
        matchingMeals: List<MealWithDetails>
    ): LocalDateTime? {
        if (matchingMeals.isEmpty()) {
            return null
        }

        val now = LocalDateTime.now()
        val today = now.dayOfWeek
        val todayDate = now.toLocalDate()
        val currentMinutes = now.hour * 60 + now.minute

        return matchingMeals
            .asSequence()
            .filter { it.meal.dayOfWeek == today }
            .filter { it.meal.timeMinutes > currentMinutes }
            .minByOrNull { it.meal.timeMinutes }
            ?.let { meal ->
                todayDate.atTime(
                    meal.meal.timeMinutes / 60,
                    meal.meal.timeMinutes % 60
                )
            }
    }
}
