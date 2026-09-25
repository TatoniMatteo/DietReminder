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
class TimePickerDialogFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTimePickerDialog_DisplaysTitle() {
        composeTestRule.setContent {
            DietTheme {
                TimePickerDialog(
                    title = "Orario pasto",
                    initialTimeMinutes = 12 * 60,
                    onDismiss = {},
                    onTimeSelected = {}
                )
            }
        }

        composeTestRule.onNode(hasText("Orario pasto"), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
