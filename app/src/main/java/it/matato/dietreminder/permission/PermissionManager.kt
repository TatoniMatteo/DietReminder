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

    private val runtimePermissions = listOf(
        Manifest.permission.POST_NOTIFICATIONS,
    )

    private val specialPermissions = listOf(
        SpecialPermission.EXACT_ALARM,
    )

    fun getMissingRuntimePermissions(context: Context): List<String> =
        runtimePermissions.filter { permission ->
            isRuntimePermissionRequired(permission) && !hasPermission(context, permission)
        }

    fun getMissingSpecialPermissions(context: Context): List<SpecialPermission> =
        specialPermissions.filter { permission ->
            !hasSpecialPermission(context, permission)
        }

    fun createSpecialPermissionIntent(
        context: Context,
        permission: SpecialPermission,
    ): Intent? = when (permission) {
        SpecialPermission.EXACT_ALARM -> Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            "package:${context.packageName}".toUri(),
        )
    }

    fun hasPermission(
        context: Context,
        permission: String,
    ): Boolean = !isRuntimePermissionRequired(permission) ||
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)

    fun notificationPermission(): String = Manifest.permission.POST_NOTIFICATIONS

    private fun hasSpecialPermission(
        context: Context,
        permission: SpecialPermission,
    ): Boolean = when (permission) {
        SpecialPermission.EXACT_ALARM -> {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        }
    }

    private fun isRuntimePermissionRequired(permission: String): Boolean =
        permission == Manifest.permission.POST_NOTIFICATIONS

    enum class SpecialPermission {
        EXACT_ALARM,
    }
}
