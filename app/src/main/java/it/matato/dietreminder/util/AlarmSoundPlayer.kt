package it.matato.dietreminder.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import it.matato.dietreminder.R

object AlarmSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null

    @Synchronized
    fun playMealAlarm(context: Context) {
        stop()

        try {
            val player = MediaPlayer()

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            player.setOnCompletionListener { completedPlayer ->
                synchronized(this) {
                    if (mediaPlayer === completedPlayer) {
                        mediaPlayer = null
                    }
                }

                completedPlayer.release()
                AppLog.d("Meal alarm sound completed")
            }

            player.setOnErrorListener { errorPlayer, what, extra ->
                AppLog.e(
                    "Meal alarm playback error: what=$what, extra=$extra"
                )

                synchronized(this) {
                    if (mediaPlayer === errorPlayer) {
                        mediaPlayer = null
                    }
                }

                errorPlayer.release()
                true
            }

            val descriptor = context.resources.openRawResourceFd(R.raw.meal_alarm)

            descriptor.use { descriptor ->
                player.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }

            player.prepare()

            mediaPlayer = player
            player.start()

            AppLog.i("Meal alarm sound started")
        } catch (e: Exception) {
            AppLog.e("Failed to play meal alarm sound", e)
            stop()
        }
    }

    @Synchronized
    fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: IllegalStateException) {
            }

            try {
                player.reset()
            } catch (_: IllegalStateException) {
            }

            try {
                player.release()
            } catch (_: Exception) {
            }
        }

        mediaPlayer = null
    }
}