package it.matato.dietreminder.ui.screens.account

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class ServerConfigurationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstScreenRequiresAddressAndNormalizesBeforeContinuing() {
        var saved: String? = null
        compose.setContent { DietTheme { ServerConfigurationContent("", false, false, false, { saved = it }, {}, {}) } }
        compose.onNodeWithTag("serverAddress").assertIsDisplayed()
        compose.onNodeWithTag("serverSave").assertIsNotEnabled()
        compose.onNodeWithTag("accountSubmit").assertDoesNotExist()
        compose.onNodeWithTag("serverAddress").performTextInput("https://example.com")
        compose.onNodeWithTag("serverSave").performClick()
        assertEquals("https://example.com/", saved)
    }

    @Test fun invalidAddressNeverLeavesSetup() {
        var saved = false
        compose.setContent { DietTheme { ServerConfigurationContent("", false, false, false, { saved = true }, {}, {}) } }
        compose.onNodeWithTag("serverAddress").performTextInput("https://example.com/api/")
        compose.onNodeWithTag("serverSave").performClick()
        assertFalse(saved)
        compose.onNodeWithTag("serverAddress").assertIsDisplayed()
    }
}
