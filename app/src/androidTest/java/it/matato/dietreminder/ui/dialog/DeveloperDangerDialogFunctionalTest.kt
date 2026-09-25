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
class DeveloperDangerDialogFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDeveloperDangerDialog_DisplaysTitleAndMessage() {
        composeTestRule.setContent {
            DietTheme {
                DeveloperDangerDialog(
                    title = "Reset",
                    message = "Are you sure?",
                    onDismiss = {},
                    onConfirm = {}
                )
            }
        }

        composeTestRule.onNode(hasText("Reset"), useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNode(hasText("Are you sure?"), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
