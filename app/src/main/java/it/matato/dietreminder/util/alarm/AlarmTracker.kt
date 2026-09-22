package it.matato.dietreminder.util.alarm

import android.content.Context
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

object AlarmTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    fun registerAlarm(context: Context, alarm: ScheduledAlarm) {
        scope.launch {
            mutex.withLock {
                val current = getAlarmsInternal(context)
                val updated = (current.filter { it.id != alarm.id } + alarm).sortedBy { it.timeMillis }

                saveAlarms(context, updated)

                AppLog.t("Alarm registered: ID=${alarm.id}, Type=${alarm.type}")
            }
        }
    }

    fun unregisterAlarm(
        context: Context,
        id: Int,
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

    fun observeAlarms(context: Context): Flow<List<ScheduledAlarm>> {
        val repo = (context.applicationContext as DietApplication).repository

        return repo.observeConfig(ConfigKey.SCHEDULED_ALARMS_REGISTRY).map { config ->
                decodeAlarms(config?.value)
            }
    }

    suspend fun getAlarms(context: Context): List<ScheduledAlarm> {
        return mutex.withLock {
            getAlarmsInternal(context)
        }
    }

    private suspend fun getAlarmsInternal(context: Context): List<ScheduledAlarm> {
        val repo = (context.applicationContext as DietApplication).repository
        val json = repo.observeConfig(ConfigKey.SCHEDULED_ALARMS_REGISTRY).first()?.value

        return decodeAlarms(json)
    }

    private fun decodeAlarms(json: String?): List<ScheduledAlarm> {
        return try {
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                Json.decodeFromString<List<ScheduledAlarm>>(json)
            }
        } catch (e: Exception) {
            AppLog.e("Failed to decode alarm registry", e)
            emptyList()
        }
    }

    private suspend fun saveAlarms(
        context: Context,
        alarms: List<ScheduledAlarm>,
    ) {
        val repo = (context.applicationContext as DietApplication).repository

        repo.saveConfig(
            ConfigKey.SCHEDULED_ALARMS_REGISTRY,
            Json.encodeToString(alarms),
        )
    }
}