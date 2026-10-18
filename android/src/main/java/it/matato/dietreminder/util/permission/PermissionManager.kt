package it.matato.dietreminder.util.permission

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import it.matato.dietreminder.BuildConfig

object PermissionManager {

    private val runtimePermissions: List<String>
        get() = buildList {
            add(Manifest.permission.POST_NOTIFICATIONS)
            // Debug builds connect to the development PC, which is a local network host.
            if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 37) {
                add(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        }

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
        permission == Manifest.permission.POST_NOTIFICATIONS ||
                (permission == Manifest.permission.ACCESS_LOCAL_NETWORK && Build.VERSION.SDK_INT >= 37)

    enum class SpecialPermission {
        EXACT_ALARM,
    }
}
