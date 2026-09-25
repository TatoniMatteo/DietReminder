package it.matato.dietreminder.util.notification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationChannelManagerTest {

    @Test
    fun testNotificationChannelManager() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val manager = NotificationChannelManager(context)
        assertNotNull(manager)
    }
}
