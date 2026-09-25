package it.matato.dietreminder.ui.screens.diets

import android.app.Application
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
class DietsFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupViewModel(): DietViewModel {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        return DietViewModel(context, fakeRepository)
    }

    @Test
    fun testUserJourney_CreateNewDiet() {
        val viewModel = setupViewModel()

        composeTestRule.setContent {
            DietTheme {
                DietsScreen(vm = viewModel, onConfigDiet = {})
            }
        }

        dietsRobot(composeTestRule) {
            verifyDietCountHeader(0)
            clickAddDiet()
            typeDietName("Dieta Proteica Sport")
            clickConfirmCreateDiet()
            verifyDietExists("Dieta Proteica Sport")
            verifyDietCountHeader(1)
        }
    }

    @Test
    fun testUserJourney_DuplicateDiet() {
        val viewModel = setupViewModel()
        viewModel.create("Dieta Base", 60)

        composeTestRule.setContent {
            DietTheme {
                DietsScreen(vm = viewModel, onConfigDiet = {})
            }
        }

        dietsRobot(composeTestRule) {
            verifyDietCountHeader(1)
            clickMoreOptionsForDiet("Dieta Base")
            clickMenuOption("Duplicate")
            verifyDietExists("Dieta Base copia")
            verifyDietCountHeader(2)
        }
    }

    @Test
    fun testUserJourney_DeleteDiet() {
        val viewModel = setupViewModel()
        viewModel.create("Dieta Temporanea", 45)

        composeTestRule.setContent {
            DietTheme {
                DietsScreen(vm = viewModel, onConfigDiet = {})
            }
        }

        dietsRobot(composeTestRule) {
            verifyDietCountHeader(1)
            clickMoreOptionsForDiet("Dieta Temporanea")
            clickMenuOption("Delete")
            verifyDietCountHeader(0)
        }
    }
}
