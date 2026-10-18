package it.matato.dietreminder.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Device appearance preference, shared by local mode and every cloud account. */
class UiLayoutPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("ui_layout", Context.MODE_PRIVATE)
    val isCompact: Boolean get() = preferences.getBoolean("compact", false)

    val compact = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "compact") trySend(isCompact)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(isCompact)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setCompact(value: Boolean) {
        preferences.edit().putBoolean("compact", value).apply()
    }
}
