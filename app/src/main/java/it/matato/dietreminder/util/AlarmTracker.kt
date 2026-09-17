package it.matato.dietreminder.util

import android.content.Context
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.data.ScheduledAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

object AlarmTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    fun registerAlarm(
        context: Context,
        alarm: ScheduledAlarm
    ) {
        scope.launch {
            mutex.withLock {
                val current = getAlarmsInternal(context)

                val updated = (current.filter { it.id != alarm.id } + alarm)
                    .sortedBy { it.timeMillis }

                saveAlarms(context, updated)

                AppLog.t("Alarm registered: ID=${alarm.id}, Type=${alarm.type}")
            }
        }
    }

    fun unregisterAlarm(
        context: Context,
        id: Int
    ) {
        scope.launch {
            mutex.withLock {
                val current = getAlarmsInternal(context)
                val updated = current.filter { it.id != id }

                saveAlarms(context, updated)

                AppLog.t("Alarm unregistered: ID=$id")
            }
        }
    }

    fun clearAll(context: Context) {
        scope.launch {
            mutex.withLock {
                saveAlarms(context, emptyList())
                AppLog.d("Alarm registry cleared")
            }
        }
    }

    suspend fun getAlarms(context: Context): List<ScheduledAlarm> {
        return mutex.withLock {
            getAlarmsInternal(context)
        }
    }

    private suspend fun getAlarmsInternal(context: Context): List<ScheduledAlarm> {
        val repo = (context.applicationContext as DietApplication).repository
        val json = repo.observeConfig("scheduled_alarms_registry").first()?.value

        return try {
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                Json.decodeFromString(json)
            }
        } catch (e: Exception) {
            AppLog.e("Failed to decode alarm registry", e)
            emptyList()
        }
    }

    private suspend fun saveAlarms(
        context: Context,
        alarms: List<ScheduledAlarm>
    ) {
        val repo = (context.applicationContext as DietApplication).repository

        repo.saveConfig(
            "scheduled_alarms_registry",
            Json.encodeToString(alarms)
        )
    }
}