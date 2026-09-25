package it.matato.dietreminder.ui.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorPickerDialogFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testColorPickerDialog_DisplaysTitle() {
        composeTestRule.setContent {
            DietTheme {
                ColorPickerDialog(
                    onDismiss = {},
                    onColorSelected = {}
                )
            }
        }

        composeTestRule.onNode(hasText("Scegli colore principale") or hasText("Choose seed color") or hasText("Choose primary color"), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
