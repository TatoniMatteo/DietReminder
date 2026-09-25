package it.matato.dietreminder.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class AppLogTest {

    @Before
    fun setUp() {
        AppLog.clear()
    }

    @Test
    fun logMethods_addEntriesToLogStream() {
        assertTrue(AppLog.entries.value.isEmpty())

        AppLog.i("Info message")
        AppLog.d("Debug message")
        AppLog.w("Warning message")
        AppLog.e("Error message", RuntimeException("Test exception"))
        AppLog.t("Trace message")

        val entries = AppLog.entries.value
        assertEquals(5, entries.size)
        assertEquals(LogLevel.TRACE, entries[0].level)
        assertEquals(LogLevel.INFO, entries[4].level)
    }

    @Test
    fun clear_resetsLogStream() {
        AppLog.i("Some log")
        assertEquals(1, AppLog.entries.value.size)

        AppLog.clear()
        assertTrue(AppLog.entries.value.isEmpty())
    }
}
