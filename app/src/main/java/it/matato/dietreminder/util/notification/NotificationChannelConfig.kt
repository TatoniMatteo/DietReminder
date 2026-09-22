package it.matato.dietreminder.util.notification

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import it.matato.dietreminder.R
import androidx.core.net.toUri

data class NotificationChannelConfig(
    val id: String,
    val version: Int,
    val nameResId: Int,
    val descriptionResId: Int,
    val importance: Int,
    val sound: Uri?,
    val audioAttributes: AudioAttributes?,
    val enableLights: Boolean,
    val enableVibration: Boolean
) {
    companion object {
        fun default(context: Context): NotificationChannelConfig {
            val sound = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.meal_alarm}".toUri()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            return NotificationChannelConfig(
                id = "diet_alarms",
                version = 2,
                nameResId = R.string.notifications_alarms,
                descriptionResId = R.string.meal_reminders_desc,
                importance = NotificationManager.IMPORTANCE_HIGH,
                sound = sound,
                audioAttributes = audioAttributes,
                enableLights = false,
                enableVibration = true
            )
        }
    }
}
