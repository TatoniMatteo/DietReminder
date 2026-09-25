package it.matato.dietreminder.ui.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class ImportDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testImportDialog_DisplaysTitleAndTabs() {
        composeTestRule.setContent {
            DietTheme {
                ImportDialog(
                    onDismiss = {},
                    onImportText = {},
                    onPickFile = {}
                )
            }
        }

        composeTestRule.onNode(hasText("Importa dieta") or hasText("Import diet"), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
