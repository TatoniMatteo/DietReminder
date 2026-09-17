package it.matato.dietreminder.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri

object PermissionManager {

    data class PermissionState(
        val permission: String, val granted: Boolean, val requestable: Boolean, val special: Boolean
    )

    fun getManifestPermissions(context: Context): List<String> {
        return context.packageManager.getPackageInfo(
                context.packageName, PackageManager.GET_PERMISSIONS
            ).requestedPermissions?.toList().orEmpty()
    }

    fun getPermissionStates(context: Context): List<PermissionState> {
        return getManifestPermissions(context).map { permission ->
            PermissionState(
                permission = permission,
                granted = isGranted(context, permission),
                requestable = canRequest(context, permission),
                special = isSpecialPermission(permission)
            )
        }
    }

    fun getNextPermissionToRequest(context: Context): String? {
        return getManifestPermissions(context).firstOrNull { permission ->
            !isGranted(context, permission) && canRequest(context, permission)
        }
    }

    fun getNextSpecialPermission(context: Context): String? {
        return getManifestPermissions(context).firstOrNull { permission ->
            !isGranted(context, permission) && isSpecialPermission(permission)
        }
    }

    fun isGranted(
        context: Context, permission: String
    ): Boolean {
        return when (permission) {
            Manifest.permission.SCHEDULE_EXACT_ALARM -> (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
            Manifest.permission.SYSTEM_ALERT_WINDOW -> Settings.canDrawOverlays(context)
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> Environment.isExternalStorageManager()
            Manifest.permission.WRITE_SETTINGS -> Settings.System.canWrite(context)
            Manifest.permission.REQUEST_INSTALL_PACKAGES -> context.packageManager.canRequestPackageInstalls()
            else -> context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun canRequest(
        context: Context, permission: String
    ): Boolean {
        if (isSpecialPermission(permission)) {
            return false
        }

        return try {
            context.packageManager.getPermissionInfo(permission, 0).protection == PermissionInfo.PROTECTION_DANGEROUS
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isSpecialPermission(permission: String): Boolean {
        return permission in SPECIAL_PERMISSIONS
    }

    fun createSpecialPermissionIntent(
        context: Context, permission: String
    ): Intent? {
        val action = SPECIAL_PERMISSION_ACTIONS[permission] ?: return null

        return Intent(action).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    private val SPECIAL_PERMISSIONS = setOf(
        Manifest.permission.SCHEDULE_EXACT_ALARM,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_SETTINGS,
        Manifest.permission.REQUEST_INSTALL_PACKAGES
    )

    private val SPECIAL_PERMISSION_ACTIONS = mapOf(
        Manifest.permission.SCHEDULE_EXACT_ALARM to Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        Manifest.permission.SYSTEM_ALERT_WINDOW to Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE to Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        Manifest.permission.WRITE_SETTINGS to Settings.ACTION_MANAGE_WRITE_SETTINGS,
        Manifest.permission.REQUEST_INSTALL_PACKAGES to Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
    )
}