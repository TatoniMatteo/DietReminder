package it.matato.dietreminder.ui.screens.update

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.R
import it.matato.dietreminder.data.model.AppVersionState
import it.matato.dietreminder.ui.dialog.UpdateDialog
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateFlowFunctionalTest {

	@get:Rule
	val composeTestRule = createComposeRule()

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun testUpdateDialog_RecentState() {
		composeTestRule.setContent {
			DietTheme {
				UpdateDialog(
					state = AppVersionState.RECENT,
					onUpdateClick = {},
					onDismiss = {},
				)
			}
		}

		composeTestRule.onNodeWithText(context.getString(R.string.update_recent_title)).assertExists()
		composeTestRule.onNodeWithText(context.getString(R.string.update_now)).assertExists()
		composeTestRule.onNodeWithText(context.getString(R.string.update_later)).assertExists()
	}

	@Test
	fun updateDialog_updateAndDismissButtonsInvokeCallbacks() {
		var updateClicked = false
		var dismissed = false
		composeTestRule.setContent {
			DietTheme {
				UpdateDialog(
					state = AppVersionState.RECENT,
					onUpdateClick = { updateClicked = true },
					onDismiss = { dismissed = true },
				)
			}
		}

		composeTestRule.onNodeWithText(context.getString(R.string.update_now)).performClick()
		composeTestRule.runOnIdle { assertTrue(updateClicked) }

		composeTestRule.onNodeWithText(context.getString(R.string.update_later)).performClick()
		composeTestRule.runOnIdle { assertTrue(dismissed) }
	}

	@Test
	fun testObsoleteScreen_State() {
		composeTestRule.setContent {
			DietTheme {
				ObsoleteScreen(
					onUpdateClick = {},
				)
			}
		}

		composeTestRule.onNodeWithText(context.getString(R.string.update_obsolete_title)).assertExists()
		composeTestRule.onNodeWithText(context.getString(R.string.update_now)).assertExists()
	}

	@Test
	fun testOfflineScreen_State() {
		var retryClicked = false
		composeTestRule.setContent {
			DietTheme {
				OfflineScreen(
					onRetryClick = { retryClicked = true },
					onContinueClick = {},
				)
			}
		}

		composeTestRule.onNodeWithText(context.getString(R.string.offline_title)).assertExists()
		composeTestRule.onNodeWithText(context.getString(R.string.retry)).performClick()
		composeTestRule.runOnIdle { assertTrue(retryClicked) }
	}

	@Test
	fun offlineScreen_continueButtonInvokesCallback() {
		var continued = false
		composeTestRule.setContent {
			DietTheme {
				OfflineScreen(
					onRetryClick = {},
					onContinueClick = { continued = true },
				)
			}
		}

		composeTestRule.onNodeWithText(context.getString(R.string.continue_offline)).performClick()
		composeTestRule.runOnIdle { assertTrue(continued) }
	}
}
