package it.matato.dietreminder.util.notification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {

    @Test
    fun testNotificationHelperContext() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertNotNull(context)
    }
}
