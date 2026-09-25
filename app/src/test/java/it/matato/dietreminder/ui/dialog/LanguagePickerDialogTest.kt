package it.matato.dietreminder.ui.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class LanguagePickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLanguagePickerDialog_DisplaysOptionsAndSelectsLanguage() {
        var selectedLang: String? = null

        composeTestRule.setContent {
            DietTheme {
                LanguagePickerDialog(
                    onDismiss = {},
                    onLanguageSelected = { selectedLang = it }
                )
            }
        }

        composeTestRule.onNode(hasText("Seleziona lingua") or hasText("Select language"), useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNode(hasText("Italiano") or hasText("Italian"), useUnmergedTree = true)
            .performClick()

        assertEquals("it", selectedLang)
    }
}
