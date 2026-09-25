package it.matato.dietreminder.util.notification

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationChannelConfigInstrumentedTest {
    @Test
    fun testNotificationChannelConfig() {
        val config = NotificationChannelConfig(id = "test_channel", version = 2)
        assertNotNull(config)
        assertEquals("test_channel", config.id)
        assertEquals(2, config.version)
    }
}
