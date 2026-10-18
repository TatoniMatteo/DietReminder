package it.matato.dietreminder.ui.screens.account

import androidx.compose.ui.test.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenFunctionalTest {
    @get:Rule
    val compose = createComposeRule()
    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun loginValidatesFieldsAndSubmitsCredentials() {
        var credentials: Pair<String, String>? = null
        compose.setContent {
            DietTheme {
                AccountAccessContent(
                    false, null, { email, password -> credentials = email to password },
                    { _, _, _ -> }, {}, { _, _ -> }, {}, {})
            }
        }
        compose.onNodeWithTag("accountSubmit").assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.account_email)).performTextInput("  dev@example.com  ")
        compose.onNodeWithText(text(R.string.account_password)).performTextInput("my test password")
        compose.onNodeWithContentDescription(text(R.string.account_show_password)).performClick()
        compose.onNodeWithContentDescription(text(R.string.account_hide_password)).assertExists()
        compose.onNodeWithTag("accountSubmit").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("dev@example.com" to "my test password", credentials) }
    }

    @Test
    fun registrationRequiresMatchingPasswords() {
        var registration: List<String>? = null
        compose.setContent {
            DietTheme {
                AccountAccessContent(
                    false,
                    null,
                    { _, _ -> },
                    { email, password, name -> registration = listOf(email, password, name) },
                    {},
                    { _, _ -> },
                    {},
                    {})
            }
        }
        compose.onNodeWithTag("accountMode_register").performClick()
        compose.onNodeWithText(text(R.string.account_email)).performTextInput("dev@example.com")
        compose.onNodeWithText(text(R.string.account_username)).performTextInput("Pixel")
        compose.onNodeWithText(text(R.string.account_password)).performScrollTo().performTextInput("My test password!")
        compose.onNodeWithText(text(R.string.account_password_confirm)).performScrollTo()
            .performTextInput("different password")
        compose.onNodeWithTag("accountSubmit").assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.account_password_confirm)).performTextReplacement("My test password!")
        compose.onNodeWithTag("accountSubmit").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("dev@example.com", "My test password!", "Pixel"), registration) }
    }

    @Test
    fun recoveryAllowsCodeEntryAndReturnToLoginOrLocalMode() {
        var local = false
        compose.setContent {
            DietTheme {
                AccountAccessContent(false, null, { _, _ -> }, { _, _, _ -> }, {}, { _, _ -> }, { local = true }, {})
            }
        }
        compose.onNodeWithText(text(R.string.account_forgot)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.account_send_code)).assertExists()
        compose.onNodeWithText(text(R.string.account_have_code)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.account_reset_code)).assertExists()
        compose.onNodeWithText(text(R.string.account_back_to_login)).performScrollTo().performClick()
        compose.onNodeWithTag("accountMode_login").assertIsSelected()
        compose.onNodeWithText(text(R.string.account_continue_local)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, local) }
    }

    @Test
    fun googleAccessDoesNotRequirePasswordFieldsAndIsDisabledWhileBusy() {
        var calls = 0
        val busy = mutableStateOf(false)
        compose.setContent {
            DietTheme {
                AccountAccessContent(
                    busy.value,
                    null,
                    { _, _ -> },
                    { _, _, _ -> },
                    {},
                    { _, _ -> },
                    {},
                    {},
                    onGoogle = { calls++ })
            }
        }
        compose.onNodeWithTag("accountSubmit").assertIsNotEnabled()
        compose.onNodeWithTag("accountGoogle").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, calls); busy.value = true }
        compose.onNodeWithTag("accountGoogle").assertIsNotEnabled()
        compose.runOnIdle { busy.value = false }
        compose.onNodeWithTag("accountMode_register").performScrollTo().performClick()
        compose.onNodeWithTag("accountGoogle").assertExists()
    }

}
