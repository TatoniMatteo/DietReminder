package it.matato.dietreminder.util.notification.receiver

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HydrationReceiverTest {

    @Test
    fun testHydrationReceiver() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val receiver = HydrationReceiver()
        assertNotNull(receiver)
        assertNotNull(context)
    }
}
