package it.matato.dietreminder.util

import android.content.Context
import androidx.core.content.edit

object FirstLaunchManager {

    private const val PREFERENCES_NAME = "app_preferences"
    private const val KEY_FIRST_LAUNCH_COMPLETED = "first_launch_completed"

    fun isFirstLaunch(context: Context): Boolean =
        !preferences(context).getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false)

    fun markCompleted(context: Context) {
        preferences(context).edit {
            putBoolean(KEY_FIRST_LAUNCH_COMPLETED, true)
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}