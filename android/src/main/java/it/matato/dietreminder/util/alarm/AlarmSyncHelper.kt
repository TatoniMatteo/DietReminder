package it.matato.dietreminder.util.alarm

import it.matato.dietreminder.data.model.resId

import android.content.Context
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.database.relation.MealWithDetails
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.util.AppLog
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AlarmSyncHelper {

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
        val configRepo = app.configRepository

        val mealRemindersEnabled =
            configRepo.observeConfig(ConfigKey.MEAL_REMINDERS_ENABLED).first()?.value != "false"

        RetiredReminderCleanup.run(context)
        AlarmScheduler.cancelAllAlarms(context)

        if (mealRemindersEnabled) {
            syncMealAlarms(context, app)
        }

        AlarmScheduler.scheduleDailySync(context)

        AppLog.i("AlarmSyncHelper: Sync completed successfully")
    }

    private suspend fun syncMealAlarms(
        context: Context,
        app: DietApplication
    ) {
        val scope = app.currentAccountScope()
        val activeDiet = app.dietRepository.active.first()

        if (activeDiet == null) {
            AppLog.w("No active diet found, meal reminders will not be scheduled")
            return
        }

        AppLog.d("Syncing meals for active diet: ${activeDiet.name}")

        val allMeals = app.dietRepository.getMeals(activeDiet.id)

        MealType.entries.forEach { type ->
            val matchingMeals = allMeals.filter { it.meal.type == type }
            val occurrence = findTodayMealOccurrence(matchingMeals)

            if (occurrence == null) {
                AppLog.d("No remaining ${type.name} meal for today")
                return@forEach
            }

            if (!activeDiet.isDayNotificationEnabled(occurrence.dayOfWeek)) {
                AppLog.d("Notifications for ${occurrence.dayOfWeek} are disabled in diet ${activeDiet.name}")
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

            if (!meal.meal.isNotificationEnabled) {
                AppLog.d("Notification disabled for meal ID ${meal.meal.id}")
                return@forEach
            }

            val timeMillis = occurrence
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val mealName = context.getString(type.resId)

            if (scope != app.currentAccountScope()) return
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
