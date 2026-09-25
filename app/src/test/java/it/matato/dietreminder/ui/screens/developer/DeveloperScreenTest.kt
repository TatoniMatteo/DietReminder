package it.matato.dietreminder.ui.screens.developer

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.repository.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class DeveloperScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDeveloperScreen_DisplaysDeveloperTitle() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        composeTestRule.setContent {
            DietTheme {
                DeveloperScreen(vm = viewModel, onBack = {})
            }
        }

        composeTestRule.onAllNodes(hasText("Developer settings") or hasText("Impostazioni sviluppatore"), useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }
}
