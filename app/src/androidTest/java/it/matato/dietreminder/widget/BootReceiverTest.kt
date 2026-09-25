package it.matato.dietreminder.widget

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BootReceiverTest {

    @Test
    fun testBootReceiverContext() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val receiver = BootReceiver()
        assertNotNull(receiver)
        assertNotNull(context)
    }
}
