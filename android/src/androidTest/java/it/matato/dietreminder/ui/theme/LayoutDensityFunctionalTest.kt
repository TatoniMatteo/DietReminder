package it.matato.dietreminder.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import it.matato.dietreminder.R
import it.matato.dietreminder.ui.AppContent
import it.matato.dietreminder.ui.navigation.AppNavigation
import it.matato.dietreminder.ui.navigation.AppNavigator
import it.matato.dietreminder.ui.screens.week.WeekScreen
import it.matato.dietreminder.data.repository.fake.FakeDietRepository
import it.matato.dietreminder.viewmodel.DietViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.matato.dietreminder.ui.screens.settings.SettingsLayoutItem
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutDensityFunctionalTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun choiceUpdatesSpacingImmediatelyWithoutShrinkingTextOrTouchTargets() {
        val preferences = UiLayoutPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
        val original = preferences.isCompact
        preferences.setCompact(false)
        var textSize = 0f
        try {
            compose.setContent {
                DietTheme {
                    val size = androidx.compose.material3.MaterialTheme.typography.bodyLarge.fontSize.value
                    SideEffect { textSize = size }
                    Column {
                        SettingsLayoutItem()
                        Box(Modifier.width(AppSpace.screen).height(10.dp).testTag("spacingProbe"))
                    }
                }
            }
            compose.onNodeWithTag("layoutStandard").assertIsSelected()
            val standard = compose.onNodeWithTag("spacingProbe").fetchSemanticsNode().boundsInRoot.width
            var standardFont = 0f
            compose.runOnIdle { standardFont = textSize }
            compose.onNodeWithTag("layoutCompact").performClick()
            compose.onNodeWithTag("layoutCompact").assertIsSelected()
            val compact = compose.onNodeWithTag("spacingProbe").fetchSemanticsNode().boundsInRoot.width
            assertTrue(compact < standard)
            compose.onNodeWithTag("layoutCompact").assertHeightIsAtLeast(48.dp)
            compose.runOnIdle {
                assertEquals(standardFont, textSize)
                assertTrue(UiLayoutPreferences(InstrumentationRegistry.getInstrumentation().targetContext).isCompact)
            }
            compose.onNodeWithTag("layoutStandard").performClick().assertIsSelected()
            assertEquals(standard, compose.onNodeWithTag("spacingProbe").fetchSemanticsNode().boundsInRoot.width)
        } finally {
            preferences.setCompact(original)
        }
    }

    @Test
    fun densityChangeAlsoUpdatesPreviouslyVisitedNavigationDestinations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = UiLayoutPreferences(context)
        val original = preferences.isCompact
        preferences.setCompact(false)
        val vm = DietViewModel(ApplicationProvider.getApplicationContext<Application>(), FakeDietRepository())
        try {
            compose.setContent {
                DietTheme {
                    val nav = rememberNavController()
                    val entry by nav.currentBackStackEntryAsState()
                    val navigator = AppNavigator(nav)
                    AppContent(
                        currentRoute = entry?.destination?.route, showBottomBar = true,
                        isOffline = false, onNavigateToRoot = navigator::navigateToRoot,
                        content = { padding -> AppNavigation(nav, vm, padding) })
                }
            }
            val standardLeft = compose.onNodeWithTag("weekTitle").fetchSemanticsNode().boundsInRoot.left
            compose.onNode(hasText(context.getString(R.string.settings)) and hasClickAction()).performClick()
            compose.onNodeWithTag("settingsPage_appearance").performClick()
            compose.onNodeWithTag("layoutCompact").performScrollTo().performClick()
            compose.onNodeWithTag("layoutCompact").assertIsSelected()
            compose.onNode(hasText(context.getString(R.string.week)) and hasClickAction()).performClick()
            val compactLeft = compose.onNodeWithTag("weekTitle").fetchSemanticsNode().boundsInRoot.left
            assertTrue("The restored Week destination must use the newly selected spacing", compactLeft < standardLeft)
        } finally {
            preferences.setCompact(original)
        }
    }

}
