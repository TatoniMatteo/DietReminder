package it.matato.dietreminder.util.notification.receiver

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseReceiverTest {

    @Test
    fun testCourseReceiver() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val receiver = CourseReceiver()
        assertNotNull(receiver)
        assertNotNull(context)
    }
}
