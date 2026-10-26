package it.matato.dietreminder.ui.screens.developer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matato.dietreminder.data.model.ScheduledAlarm
import it.matato.dietreminder.ui.theme.DietTheme
import it.matato.dietreminder.util.LogEntry
import it.matato.dietreminder.util.LogLevel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperDiagnosticsFunctionalTest {
    @get:Rule val compose = createComposeRule()

    @Test fun logsCanBeSearchedFilteredAndCleared() {
        val source = mutableStateOf(listOf(
            LogEntry("12:01:00", LogLevel.ERROR, "Sync failed: connection unavailable"),
            LogEntry("12:00:00", LogLevel.INFO, "Sync completed"),
            LogEntry("11:59:00", LogLevel.DEBUG, "Loading meals"),
        ))
        compose.setContent {
            var level by remember { mutableStateOf(LogLevel.TRACE) }
            DietTheme { Surface(color = MaterialTheme.colorScheme.surface) { DeveloperLogSection(source.value, level, { level = it }, { source.value = emptyList() }) } }
        }
        screenshot("developer-logs")
        compose.onNodeWithTag("developerLogSearch").performTextInput("SYNC")
        compose.onNodeWithText("Loading meals").assertDoesNotExist()
        compose.onNodeWithText("Sync completed").assertIsDisplayed()
        compose.onNodeWithTag("developerLogLevel").performClick()
        compose.onNodeWithTag("developerLogLevel_ERROR").performClick()
        compose.onNodeWithText("Sync completed").assertDoesNotExist()
        compose.onNodeWithText("Sync failed: connection unavailable").assertIsDisplayed()
        compose.onNodeWithTag("developerClearLogs").performClick()
        compose.onNodeWithText("Sync failed: connection unavailable").assertDoesNotExist()
        compose.onNodeWithTag("developerClearLogs").assertIsNotEnabled()
    }

    @Test fun longAlarmsRemainFullyLaidOutAtLargeFontAndTheLastAlarmIsReachable() {
        val label = "Pranzo: pasta al pomodoro con verdure di stagione, frutta fresca e pane integrale."
        val alarms = (1..30).map { ScheduledAlarm(it, "LUNCH", 1793098800000L + it * 60000, "$label Evento $it") }
        var cancelled: Int? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                DietTheme {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.width(280.dp).height(700.dp)) {
                        DeveloperAlarmsSection(alarms.reversed(), { cancelled = it.id }, {}, {})
                    }
                    }
                }
            }
        }
        screenshot("developer-alarms-large-font")
        compose.onNodeWithTag("developerAlarmsList").performScrollToNode(hasTestTag("developerAlarm_30"))
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("$label Evento 30").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse(layouts.single().hasVisualOverflow)
        compose.onNodeWithText("$label Evento 30").assertIsDisplayed()
        compose.onNodeWithTag("developerCancelAlarm_30").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(30, cancelled) }
        screenshot("developer-alarms-last-entry")
    }

    @Test fun longLogMessagesWrapWithoutTruncation() {
        val message = "Unable to synchronize the meal plan.\n" + "Connection details and diagnostic information. ".repeat(12)
        compose.setContent { DietTheme { DeveloperLogSection(listOf(LogEntry("12:00:00.000", LogLevel.ERROR, message)), LogLevel.TRACE, {}, {}) } }
        compose.onNodeWithTag("developerLogsList").performScrollToNode(hasText(message))
        compose.onNodeWithTag("developerLogEntry_0").performClick()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(message, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow)
        assertTrue(layouts.single().lineCount > 3)
        screenshot("developer-long-log")
    }
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

}
