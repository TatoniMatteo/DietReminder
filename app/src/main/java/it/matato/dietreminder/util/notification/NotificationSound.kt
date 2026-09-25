package it.matato.dietreminder.util.notification

import android.media.AudioAttributes
import android.net.Uri

sealed interface NotificationSound {
    data object Default : NotificationSound

    data object Silent : NotificationSound

    data class Custom(
        val uri: Uri,
        val audioAttributes: AudioAttributes,
    ) : NotificationSound
}
