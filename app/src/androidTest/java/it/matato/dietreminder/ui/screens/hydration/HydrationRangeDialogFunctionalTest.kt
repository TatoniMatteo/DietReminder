package it.matato.dietreminder.ui.screens.hydration

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.ui.theme.DietTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class HydrationRangeDialogFunctionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHydrationRangeDialogContent_Exists() {
        composeTestRule.setContent {
            val startState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
            val endState = rememberTimePickerState(initialHour = 18, initialMinute = 0, is24Hour = true)
            DietTheme {
                AddHydrationRangeDialogContent(
                    startState = startState,
                    endState = endState,
                    onDismiss = {},
                    onAdd = {}
                )
            }
        }

        composeTestRule.onNode(isRoot()).assertExists()
    }
}
