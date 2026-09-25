package it.matato.dietreminder.util.notification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.R
import it.matato.dietreminder.TestDietApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class NotificationChannelConfigTest {

    @Test
    fun defaultChannelConfig_hasExpectedDefaults() {
        val config = NotificationChannelConfig()
        assertEquals("default_alarms", config.id)
        assertEquals(1, config.version)
        assertEquals(R.string.notifications_alarms, config.nameResId)
    }

    @Test
    fun customSound_createsCustomNotificationSound() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val customSound = NotificationChannelConfig.customSound(context, R.raw.hydration_alarm)

        assertNotNull(customSound)
        assertNotNull(customSound.uri)
        assertNotNull(customSound.audioAttributes)
    }
}
