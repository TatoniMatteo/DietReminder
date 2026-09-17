package it.matato.dietreminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.HydrationRange
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.data.model.ScheduledAlarm
import java.time.Instant
import java.util.Calendar
import kotlinx.serialization.json.Json

object AlarmScheduler {

    private const val HYDRATION_ALARM_ID = 999
    private const val DAILY_SYNC_ALARM_ID = 1000

    fun scheduleExactAlarm(
        context: Context,
        type: MealType,
        timeMillis: Long,
        title: String,
        message: String,
        extraData: Map<String, String> = emptyMap()
    ) {
        if (!isToday(timeMillis)) {
            AppLog.w(
                "Skipping ${type.name} alarm: ${Instant.ofEpochMilli(timeMillis)} is not today"
            )
            cancelAlarm(context, type.ordinal)
            return
        }

        if (timeMillis <= System.currentTimeMillis()) {
            AppLog.w(
                "Skipping ${type.name} alarm: ${Instant.ofEpochMilli(timeMillis)} is in the past"
            )
            cancelAlarm(context, type.ordinal)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_TYPE, type.name)

            extraData.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val triggerPendingIntent = PendingIntent.getBroadcast(
            context,
            type.ordinal,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(
            Intent.ACTION_VIEW, createMealDeepLink(extraData)
        ).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val showPendingIntent = PendingIntent.getActivity(
            context,
            type.ordinal,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        AppLog.d("Scheduling ${type.name} alarm clock at ${Instant.ofEpochMilli(timeMillis)}")

        scheduleAlarmClock(
            alarmManager = alarmManager,
            timeMillis = timeMillis,
            triggerPendingIntent = triggerPendingIntent,
            showPendingIntent = showPendingIntent,
            label = message
        )

        AlarmTracker.registerAlarm(
            context, ScheduledAlarm(
                id = type.ordinal, type = type.name, timeMillis = timeMillis, label = title
            )
        )
    }

    fun scheduleHydrationAlarm(
        context: Context, intervalMinutes: Int, ranges: List<HydrationRange>
    ) {
        if (ranges.isEmpty()) {
            AppLog.w("No hydration ranges configured, skipping alarm")
            cancelAlarm(context, HYDRATION_ALARM_ID)
            return
        }

        if (intervalMinutes <= 0) {
            AppLog.w("Invalid hydration interval: $intervalMinutes minutes")
            cancelAlarm(context, HYDRATION_ALARM_ID)
            return
        }

        val normalizedRanges = ranges.filter { it.startMinutes < it.endMinutes }.sortedBy { it.startMinutes }

        if (normalizedRanges.isEmpty()) {
            AppLog.w("No valid hydration ranges configured, skipping alarm")
            cancelAlarm(context, HYDRATION_ALARM_ID)
            return
        }

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val nextTriggerMillis = calculateNextHydrationTrigger(
            now = now, currentMinutes = currentMinutes, intervalMinutes = intervalMinutes, ranges = normalizedRanges
        )

        if (nextTriggerMillis == null) {
            AppLog.d("No hydration alarm can be scheduled today")
            cancelAlarm(context, HYDRATION_ALARM_ID)
            return
        }

        if (!isToday(nextTriggerMillis)) {
            AppLog.w("Skipping hydration alarm because the trigger is not today")
            cancelAlarm(context, HYDRATION_ALARM_ID)
            return
        }

        if (nextTriggerMillis <= System.currentTimeMillis()) {
            AppLog.w("Skipping hydration alarm because the trigger is in the past")
            cancelAlarm(context, HYDRATION_ALARM_ID)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, context.getString(R.string.hydration_notification_title))
            putExtra(EXTRA_MESSAGE, context.getString(R.string.hydration_notification_message))
            putExtra(EXTRA_TYPE, TYPE_HYDRATION)
            putExtra(EXTRA_INTERVAL, intervalMinutes.toString())
            putExtra(EXTRA_RANGES_JSON, Json.encodeToString(normalizedRanges))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            HYDRATION_ALARM_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        AppLog.d("Scheduling hydration alarm at ${Instant.ofEpochMilli(nextTriggerMillis)}")
        scheduleExactAlarmManagerAlarm(
            alarmManager = alarmManager,
            timeMillis = nextTriggerMillis,
            pendingIntent = pendingIntent
        )

        AlarmTracker.registerAlarm(
            context, ScheduledAlarm(
                id = HYDRATION_ALARM_ID,
                type = TYPE_HYDRATION,
                timeMillis = nextTriggerMillis,
                label = context.getString(R.string.hydration_notification_title)
            )
        )
    }

    fun scheduleDailySync(context: Context) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val timeMillis = calendar.timeInMillis
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_DAILY_SYNC)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SYNC_ALARM_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        AppLog.d("Scheduling daily sync at ${Instant.ofEpochMilli(timeMillis)}")
        scheduleExactAlarmManagerAlarm(alarmManager = alarmManager, timeMillis = timeMillis, pendingIntent = pendingIntent)

        AlarmTracker.registerAlarm(
            context, ScheduledAlarm(
                id = DAILY_SYNC_ALARM_ID, type = TYPE_DAILY_SYNC, timeMillis = timeMillis, label = "Daily alarm sync"
            )
        )
    }

    fun cancelAlarm(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            AppLog.i("Alarm cancelled: ID=$id")
        }

        AlarmTracker.unregisterAlarm(context, id)
    }

    fun cancelAllAlarms(context: Context) {
        AppLog.d("Cancelling all alarms")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        MealType.entries.forEach { type ->
            cancelAlarmManagerAlarm(
                alarmManager = alarmManager, context = context, id = type.ordinal
            )
        }

        cancelAlarmManagerAlarm(alarmManager = alarmManager, context = context, id = HYDRATION_ALARM_ID)
        cancelAlarmManagerAlarm(alarmManager = alarmManager, context = context, id = DAILY_SYNC_ALARM_ID)

        AlarmTracker.clearAll(context)
    }

    private fun calculateNextHydrationTrigger(
        now: Calendar, currentMinutes: Int, intervalMinutes: Int, ranges: List<HydrationRange>
    ): Long? {
        val activeRange = ranges.firstOrNull {
            currentMinutes >= it.startMinutes && currentMinutes < it.endMinutes
        }

        if (activeRange != null) {
            val candidateMillis = now.timeInMillis + intervalMinutes * 60 * 1000L

            val candidate = Calendar.getInstance().apply {
                timeInMillis = candidateMillis
            }

            val candidateMinutes = candidate.get(Calendar.HOUR_OF_DAY) * 60 + candidate.get(Calendar.MINUTE)

            if (candidateMinutes < activeRange.endMinutes && isToday(candidateMillis)) {
                return candidateMillis
            }
        }

        val nextRange = ranges.firstOrNull {
            it.startMinutes > currentMinutes
        }

        if (nextRange == null) {
            return null
        }

        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextRange.startMinutes / 60)
            set(Calendar.MINUTE, nextRange.startMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun scheduleAlarmClock(
        alarmManager: AlarmManager,
        timeMillis: Long,
        triggerPendingIntent: PendingIntent,
        showPendingIntent: PendingIntent,
        label: String
    ) {
        if (alarmManager.canScheduleExactAlarms()) {
            AppLog.t("Using setAlarmClock for user alarm")
            val alarmClockInfo = AlarmManager.AlarmClockInfo(timeMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, triggerPendingIntent)
        } else {
            AppLog.w("Exact alarms not allowed, falling back to setAndAllowWhileIdle")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, triggerPendingIntent)
        }

        AppLog.d("Alarm clock scheduled: $label")
    }

    private fun scheduleExactAlarmManagerAlarm(
        alarmManager: AlarmManager, timeMillis: Long, pendingIntent: PendingIntent
    ) {
        if (alarmManager.canScheduleExactAlarms()) {
            AppLog.t("Using setExactAndAllowWhileIdle")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        } else {
            AppLog.w("Exact alarms not allowed, falling back to setAndAllowWhileIdle")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    private fun cancelAlarmManagerAlarm(
        alarmManager: AlarmManager, context: Context, id: Int
    ) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    private fun createMealDeepLink(
        extraData: Map<String, String>
    ) = run {
        val mealId = extraData[EXTRA_MEAL_ID]?.toLongOrNull()
        val dayName = extraData[EXTRA_DAY_NAME]

        if (mealId != null && !dayName.isNullOrBlank()) {
            "dietreminder://week?mealId=$mealId&dayName=$dayName".toUri()
        } else {
            "dietreminder://week".toUri()
        }
    }

    private fun isToday(timeMillis: Long): Boolean {
        val target = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
        }

        val today = Calendar.getInstance()

        return target.get(Calendar.YEAR) == today.get(Calendar.YEAR) && target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private const val TYPE_HYDRATION = "HYDRATION"
    private const val TYPE_DAILY_SYNC = "DAILY_SYNC"

    private const val EXTRA_TYPE = "type"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_MESSAGE = "message"
    private const val EXTRA_INTERVAL = "interval"
    private const val EXTRA_RANGES_JSON = "ranges_json"
    private const val EXTRA_MEAL_ID = "meal_id"
    private const val EXTRA_DAY_NAME = "day_name"
}
