package it.matato.dietreminder.ui

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.repository.FakeDietRepository
import it.matato.dietreminder.ui.navigation.AppNavigation
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupViewModel(): DietViewModel {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepository = FakeDietRepository()
        return DietViewModel(context, fakeRepository)
    }

    @Test
    fun testAppNavigation_SwitchTabs() {
        val viewModel = setupViewModel()

        composeTestRule.setContent {
            val navController = rememberNavController()
            DietTheme {
                AppContent(
                    currentRoute = "WeekRoute",
                    showBottomBar = true,
                    onNavigateToRoot = { destination ->
                        navController.navigate(destination.route())
                    },
                    content = { padding ->
                        AppNavigation(
                            navController = navController,
                            vm = viewModel,
                            contentPadding = padding
                        )
                    }
                )
            }
        }

        appRobot(composeTestRule) {
            navigateToDiets()
            navigateToHydration()
            navigateToSettings()
            navigateToWeek()
        }
    }
}
