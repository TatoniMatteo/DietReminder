package it.matato.dietreminder.permission

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object PermissionManager {

    /**
     * Runtime permissions that can be requested directly by the application.
     *
     * Add new permissions here when they are introduced in the manifest and require
     * an explicit runtime request.
     */
    private val runtimePermissions = listOf(
        Manifest.permission.POST_NOTIFICATIONS,
    )

    /**
     * Special permissions that require opening an Android system settings screen.
     *
     * Add new permissions here when they require a dedicated system settings screen.
     */
    private val specialPermissions = listOf(
        SpecialPermission.EXACT_ALARM,
    )

    /**
     * Returns the first runtime permission that has not yet been granted.
     */
    fun getNextPermissionToRequest(context: Context): String? = runtimePermissions.firstOrNull { permission ->
        isRuntimePermissionRequired(permission) && !hasPermission(context, permission)
    }

    /**
     * Returns the first special permission that has not yet been granted.
     */
    fun getNextSpecialPermission(context: Context): SpecialPermission? = specialPermissions.firstOrNull { permission ->
        !hasSpecialPermission(context, permission)
    }

    /**
     * Creates the Android settings Intent required to grant the given special permission.
     */
    fun createSpecialPermissionIntent(context: Context, permission: SpecialPermission): Intent? = when (permission) {
        SpecialPermission.EXACT_ALARM -> {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                "package:${context.packageName}".toUri(),
            )
        }
    }

    /**
     * Checks whether a normal Android runtime permission has been granted.
     */
    fun hasPermission(context: Context, permission: String): Boolean =
        !isRuntimePermissionRequired(permission) || ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Checks whether notification permission has been granted.
     *
     * On Android versions before 13, notifications do not require a runtime permission.
     */
    fun hasNotificationPermission(context: Context): Boolean = hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)

    /**
     * Returns the notification permission name.
     */
    fun notificationPermission(): String = Manifest.permission.POST_NOTIFICATIONS

    private fun hasSpecialPermission(context: Context, permission: SpecialPermission): Boolean = when (permission) {
        SpecialPermission.EXACT_ALARM -> {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        }
    }

    private fun isRuntimePermissionRequired(permission: String): Boolean = permission == Manifest.permission.POST_NOTIFICATIONS

    enum class SpecialPermission {
        EXACT_ALARM,
    }
}