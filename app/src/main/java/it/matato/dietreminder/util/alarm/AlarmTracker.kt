package it.matato.dietreminder.util.alarm

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

object AlarmTracker {

	private const val PREFERENCES_NAME = "scheduled_alarms"
	private const val REGISTRY_KEY = "registry"

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
		val preferences = preferences(context)
		return callbackFlow {
			val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
				if (key == REGISTRY_KEY) {
					trySend(decodeAlarms(prefs.getString(REGISTRY_KEY, null)))
				}
			}
			preferences.registerOnSharedPreferenceChangeListener(listener)
			trySend(decodeAlarms(preferences.getString(REGISTRY_KEY, null)))
			awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
		}.distinctUntilChanged()
	}

	suspend fun getAlarms(context: Context): List<ScheduledAlarm> {
		return mutex.withLock {
			getAlarmsInternal(context)
		}
	}

	private suspend fun getAlarmsInternal(context: Context): List<ScheduledAlarm> {
		return decodeAlarms(preferences(context).getString(REGISTRY_KEY, null))
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
		preferences(context).edit {
			putString(REGISTRY_KEY, Json.encodeToString(alarms))
		}
	}

	private fun preferences(context: Context): SharedPreferences =
		context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
