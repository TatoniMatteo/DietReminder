package it.matato.dietreminder.ui

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.test.platform.app.InstrumentationRegistry
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.navigation.AppNavigator
import kotlinx.coroutines.runBlocking
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
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
					isOffline = false,
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
			navigateToShoppingList()
			navigateToSettings()
			navigateToWeek()
		}
	}
    @Test
    fun tappingSelectedShoppingTabReturnsFromDetailToLists() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repository = FakeDietRepository()
        runBlocking { repository.createShoppingList("Spesa nav test") }
        val vm = DietViewModel(context, repository)
        composeTestRule.setContent {
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val navigator = AppNavigator(nav)
            DietTheme {
                AppContent(currentRoute = entry?.destination?.route, showBottomBar = true,
                    isOffline = false, onNavigateToRoot = navigator::navigateToRoot,
                    content = { padding -> AppNavigation(nav, vm, padding) })
            }
        }
        val resources = InstrumentationRegistry.getInstrumentation().targetContext
        val shopping = resources.getString(R.string.nav_shopping_lists)
        composeTestRule.onNode(hasText(shopping) and hasClickAction()).performClick()
        composeTestRule.onNodeWithText("Spesa nav test").performClick()
        composeTestRule.onNodeWithContentDescription(resources.getString(R.string.add_item)).assertIsDisplayed()
        composeTestRule.onNode(hasText(shopping) and hasClickAction()).performClick()
        composeTestRule.onNodeWithText(resources.getString(R.string.shopping_lists)).assertIsDisplayed()
    }

}
