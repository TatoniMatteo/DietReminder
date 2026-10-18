package it.matato.dietreminder.util.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit

class NotificationChannelManager(
    private val context: Context,
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun ensureChannel(config: NotificationChannelConfig) {
        val installedVersion = preferences.getInt(
            versionKey(config.id),
            NO_VERSION,
        )

        val channelExists = notificationManager.getNotificationChannel(config.id) != null

        if (channelExists && installedVersion == config.version) {
            return
        }

        if (channelExists) {
            notificationManager.deleteNotificationChannel(config.id)
        }

        createChannel(config)

        preferences.edit {
            putInt(versionKey(config.id), config.version)
        }
    }

    private fun createChannel(config: NotificationChannelConfig) {
        val channel = NotificationChannel(
            config.id,
            context.getString(config.nameResId),
            config.importance,
        ).apply {
            description = context.getString(config.descriptionResId)
            enableLights(config.enableLights)
            enableVibration(config.enableVibration)

            when (val sound = config.sound) {
                NotificationSound.Default -> {
                    // Let Android use its default notification sound.
                }

                NotificationSound.Silent -> {
                    setSound(null, null)
                }

                is NotificationSound.Custom -> {
                    setSound(sound.uri, sound.audioAttributes)
                }
            }
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun versionKey(channelId: String): String =
        "$CHANNEL_VERSION_PREFIX$channelId"

    companion object {
        private const val PREFERENCES_NAME = "notification_channels"
        private const val CHANNEL_VERSION_PREFIX = "channel_version_"
        private const val NO_VERSION = -1
    }
}
