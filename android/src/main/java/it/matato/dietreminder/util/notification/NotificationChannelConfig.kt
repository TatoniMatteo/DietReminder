package it.matato.dietreminder.util.notification

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import androidx.core.net.toUri
import it.matato.dietreminder.R

data class NotificationChannelConfig(
    val id: String = "default_alarms",
    val version: Int = 1,
    val nameResId: Int = R.string.notifications_alarms,
    val descriptionResId: Int = R.string.meal_reminders_desc,
    val importance: Int = NotificationManager.IMPORTANCE_HIGH,
    val sound: NotificationSound = NotificationSound.Default,
    val enableLights: Boolean = false,
    val enableVibration: Boolean = true,
) {
    companion object {
        fun customSound(
            context: Context,
            soundResId: Int,
        ): NotificationSound.Custom {
            val sound = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$soundResId"
                .toUri()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            return NotificationSound.Custom(sound, audioAttributes)
        }
    }
}
