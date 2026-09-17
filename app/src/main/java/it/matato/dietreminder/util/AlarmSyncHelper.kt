package it.matato.dietreminder.util

import android.content.Context
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.data.model.MealType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object AlarmSyncHelper {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncAlarms(context: Context) {
        scope.launch {
            try {
                doSync(context)
            } catch (e: Exception) {
                AppLog.e("Failed to sync alarms", e)
            }
        }
    }

    suspend fun doSync(context: Context) {
        AppLog.i("AlarmSyncHelper: Sync starting...")

        val app = context.applicationContext as DietApplication
        val repo = app.repository

        val mealRemindersEnabled = repo.observeConfig("meal_reminders_enabled").first()?.value != "false"
        val hydrationEnabled = repo.observeConfig("hydration_enabled").first()?.value == "true"

        AppLog.d(
            "Current configuration: mealReminders=$mealRemindersEnabled, hydration=$hydrationEnabled"
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

            val mealId = matchingMeals
                .find {
                    it.meal.dayOfWeek == occurrence.dayOfWeek &&
                            it.meal.timeMinutes == occurrence.hour * 60 + occurrence.minute
                }
                ?.meal
                ?.id

            if (mealId == null) {
                AppLog.w("Unable to find meal ID for ${type.name} at $occurrence")
                return@forEach
            }

            val timeMillis = occurrence
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val mealName = context.getString(type.resId)

            AlarmScheduler.scheduleExactAlarm(
                context = context,
                type = type,
                timeMillis = timeMillis,
                title = context.getString(R.string.it_is_time_to_eat),
                message = context.getString(R.string.it_is_moment_of, mealName),
                extraData = mapOf(
                    "meal_id" to mealId.toString(),
                    "day_name" to occurrence.dayOfWeek.name
                )
            )
        }
    }

    private suspend fun syncHydrationAlarm(context: Context, app: DietApplication) {
        val hydrationRangesJson = app.repository.observeConfig("hydration_ranges").first()?.value
        val hydrationRanges = hydrationRangesJson?.let { json ->
            try {
                Json.decodeFromString<List<HydrationRange>>(json)
            } catch (e: Exception) {
                AppLog.e("Failed to decode hydration ranges", e); null
            }
        } ?: listOf(HydrationRange(startMinutes = 8 * 60, endMinutes = 22 * 60))

        val activeDays = app.repository.observeConfig("hydration_days").first()?.value?.split(",")?.mapNotNull {
            try {
                DayOfWeek.valueOf(it.trim())
            } catch (_: Exception) {
                null
            }
        }?.toSet() ?: DayOfWeek.entries.toSet()

        val interval = app.repository.observeConfig("hydration_interval").first()?.value?.toIntOrNull() ?: 120
        val today = LocalDate.now().dayOfWeek
        if (!activeDays.contains(today)) {
            AppLog.i("Hydration reminder skipped: today ($today) is not an active day")
            return
        }

        AppLog.t("Scheduling hydration for today ($today) with interval $interval min")

        AlarmScheduler.scheduleHydrationAlarm(
            context = context,
            intervalMinutes = interval,
            ranges = hydrationRanges
        )
    }

    private fun findTodayMealOccurrence(matchingMeals: List<MealWithDetails>): LocalDateTime? {
        if (matchingMeals.isEmpty()) {
            return null
        }

        val now = LocalDateTime.now()
        val today = now.dayOfWeek
        val todayDate = now.toLocalDate()
        val currentMinutes = now.hour * 60 + now.minute

        return matchingMeals
            .filter { it.meal.dayOfWeek == today }
            .filter { it.meal.timeMinutes > currentMinutes }
            .minOfOrNull {
                todayDate.atTime(
                    it.meal.timeMinutes / 60,
                    it.meal.timeMinutes % 60
                )
            }
    }
}