package it.matato.dietreminder.permission

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.TestDietApplication
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = TestDietApplication::class)
class PermissionRequesterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPermissionRequester_CanBeInvoked() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var requesterLambda: (() -> Unit)? = null

        composeTestRule.setContent {
            DietTheme {
                requesterLambda = rememberPermissionRequester(
                    context = context,
                    onCompleted = {}
                )
            }
        }

        assertNotNull(requesterLambda)
        requesterLambda?.invoke()
    }
}
