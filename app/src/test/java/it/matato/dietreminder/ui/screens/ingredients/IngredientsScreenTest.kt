package it.matato.dietreminder.ui.screens.ingredients

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class IngredientsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testIngredientsScreen_DisplaysTitle() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        val viewModel = DietViewModel(context, fakeRepository)

        composeTestRule.setContent {
            DietTheme {
                IngredientsScreen(vm = viewModel)
            }
        }

        composeTestRule.onNode(hasText("Lista ingredienti") or hasText("Ingredients list"), useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
