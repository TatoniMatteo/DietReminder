package it.matato.dietreminder.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLogInstrumentedTest {
    @Test
    fun testAppLogMessages() {
        AppLog.d("Debug message test")
        AppLog.i("Info message test")
        AppLog.w("Warning message test")
        AppLog.e("Error message test")
        assertNotNull(AppLog.entries)
    }
}
