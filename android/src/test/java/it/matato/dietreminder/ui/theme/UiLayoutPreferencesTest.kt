package it.matato.dietreminder.ui.theme

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class UiLayoutPreferencesTest {
    @Test
    fun compactChoiceSurvivesNewPreferenceInstancesAndCanBeRestored() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val original = UiLayoutPreferences(context).isCompact
        try {
            UiLayoutPreferences(context).setCompact(true)
            assertTrue(UiLayoutPreferences(context).isCompact)
            assertTrue(UiLayoutPreferences(context).compact.first())
            UiLayoutPreferences(context).setCompact(false)
            assertFalse(UiLayoutPreferences(context).isCompact)
            assertFalse(UiLayoutPreferences(context).compact.first())
        } finally {
            UiLayoutPreferences(context).setCompact(original)
        }
    }
}
