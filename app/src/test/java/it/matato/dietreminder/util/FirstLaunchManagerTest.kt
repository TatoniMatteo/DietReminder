package it.matato.dietreminder.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class FirstLaunchManagerTest {

    @Test
    fun isFirstLaunch_and_markCompleted() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        assertTrue(FirstLaunchManager.isFirstLaunch(context))

        FirstLaunchManager.markCompleted(context)

        assertFalse(FirstLaunchManager.isFirstLaunch(context))
    }
}
