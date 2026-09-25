package it.matato.dietreminder.ui.screens.developer

import android.app.Application
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.repository.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperScreenFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDeveloperScreen_Exists() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        composeTestRule.setContent {
            DietTheme {
                DeveloperScreen(vm = viewModel, onBack = {})
            }
        }

        composeTestRule.onNode(isRoot()).assertExists()
    }
}
