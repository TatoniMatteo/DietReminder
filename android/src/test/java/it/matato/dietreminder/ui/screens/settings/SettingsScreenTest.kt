package it.matato.dietreminder.ui.screens.settings

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.ConfigKey
import kotlinx.coroutines.runBlocking
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.viewmodel.DietViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class SettingsScreenTest {

	@get:Rule
	val composeTestRule = createComposeRule()

	@Test
	fun testSettingsScreen_DisplaysSections() {
		val context = ApplicationProvider.getApplicationContext<Application>()
		val fakeRepository = FakeDietRepository()
		val viewModel = DietViewModel(context, fakeRepository)

		composeTestRule.setContent {
			DietTheme {
				SettingsScreen(vm = viewModel)
			}
		}

		composeTestRule.onNode(hasText("Impostazioni") or hasText("Settings"), useUnmergedTree = true)
			.assertIsDisplayed()
	}

    @Test
    fun rapidVersionTapsReplaceTheCountdownAndActivationMessage() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val vm = DietViewModel(context, FakeDietRepository())
        composeTestRule.setContent { DietTheme { SettingsScreen(vm, section = "about") } }
        val version = composeTestRule.onNodeWithText(context.getString(R.string.version))
        repeat(2) { version.performClick() }
        for (remaining in 4 downTo 1) {
            version.performClick()
            composeTestRule.onNodeWithText(context.resources.getQuantityString(R.plurals.developer_clicks_remaining, remaining, remaining)).assertIsDisplayed()
            if (remaining < 4) composeTestRule.onNodeWithText(context.resources.getQuantityString(R.plurals.developer_clicks_remaining, remaining + 1, remaining + 1)).assertDoesNotExist()
        }
        version.performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.developer_mode_activated)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.resources.getQuantityString(R.plurals.developer_clicks_remaining, 1, 1)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun notificationSettingsOfferTheCommonShoppingTimePicker() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = FakeDietRepository()
        runBlocking { repo.saveConfig(ConfigKey.SHOPPING_REMINDER_TIME, "1170") }
        val vm = DietViewModel(context, repo)
        composeTestRule.setContent { DietTheme { SettingsScreen(vm, section = "notifications") } }
        composeTestRule.onNodeWithText(context.getString(R.string.shopping_reminder_time_summary)).assertIsDisplayed()
        composeTestRule.onNodeWithText("19:30").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.shopping_reminder_time)).performClick()
        composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("saveButton").assertDoesNotExist()
    }
    @Test
    fun developerHubEntryNavigatesDirectlyToDeveloperTools() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = FakeDietRepository()
        runBlocking { repo.saveConfig(ConfigKey.DEVELOPER_MODE, "true") }
        var opened = false
        val vm = DietViewModel(context, repo)
        composeTestRule.setContent { DietTheme { SettingsScreen(vm, onNavigateToDeveloper = { opened = true }) } }
        composeTestRule.onNodeWithTag("settingsPage_developer").performScrollTo().performClick()
        composeTestRule.runOnIdle { org.junit.Assert.assertTrue(opened) }
    }

}
