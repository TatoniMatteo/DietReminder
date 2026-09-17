package it.matato.dietreminder.util

import android.util.Log
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel(val priority: Int) { 
    ERROR(4), 
    WARN(3), 
    INFO(2), 
    DEBUG(1), 
    TRACE(0) 
}

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

object AppLog {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun e(message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, message + (throwable?.let { " | ${it.localizedMessage}" } ?: ""))
        Log.e("DietApp", message, throwable)
    }

    fun w(message: String) {
        log(LogLevel.WARN, message)
        Log.w("DietApp", message)
    }

    fun i(message: String) {
        log(LogLevel.INFO, message)
        Log.i("DietApp", message)
    }

    fun d(message: String) {
        log(LogLevel.DEBUG, message)
        Log.d("DietApp", message)
    }

    fun t(message: String) {
        log(LogLevel.TRACE, message)
        Log.v("DietApp", message)
    }

    private fun log(level: LogLevel, message: String) {
        val timestamp = LocalTime.now().format(formatter)
        val entry = LogEntry(timestamp, level, message)
        _entries.value = (listOf(entry) + _entries.value).take(500)
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
