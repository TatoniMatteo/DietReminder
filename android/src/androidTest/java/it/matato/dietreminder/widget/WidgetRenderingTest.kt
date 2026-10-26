package it.matato.dietreminder.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetHostView
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.activity.ComponentActivity
import androidx.test.platform.app.InstrumentationRegistry
import it.matato.dietreminder.R
import it.matato.dietreminder.util.UpdateManagerStatus
import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.database.relation.*
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.theme.brandColorScheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WidgetRenderingTest {
    @get:Rule val activity = ActivityScenarioRule(ComponentActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private class Fixture(private val content: @Composable () -> Unit): GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) { provideContent { content() } }
    }
    private fun render(size: DpSize, snapshot: String? = null, content: @Composable () -> Unit): List<String> = runBlocking {
        val views=Fixture(content).compose(context,size=size)
        lateinit var widgetView: View
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).toInt()
        val height = (size.height.value * density).toInt()
        activity.scenario.onActivity { host ->
            val parent = FrameLayout(host)
            val provider = AppWidgetManager.getInstance(context).getInstalledProvidersForPackage(context.packageName, null)
                .first { it.provider == ComponentName(context, ShoppingWidgetReceiver::class.java) }
            val widgetHost = AppWidgetHostView(host)
            widgetHost.setAppWidget(0, provider)
            widgetHost.setPadding(0, 0, 0, 0)
            widgetHost.updateAppWidget(views)
            widgetView = widgetHost
            parent.addView(widgetView, FrameLayout.LayoutParams(width, height))
            host.setContentView(parent)
        }
        instrumentation.waitForIdleSync()
        // Attached RemoteViews load their collection children asynchronously.
        var result = emptyList<String>()
        for (attempt in 0 until 10) {
            instrumentation.runOnMainSync { result = texts(widgetView) }
            if (result.contains("Mele") || result.contains("Pasta al pesto")) break
            Thread.sleep(50)
            instrumentation.waitForIdleSync()
        }
        instrumentation.runOnMainSync {
            assertEquals(width, widgetView.measuredWidth)
            assertEquals(height, widgetView.measuredHeight)
            result = texts(widgetView)
        }
        if (snapshot != null) {
            val screen = instrumentation.uiAutomation.takeScreenshot()
            val location = IntArray(2)
            instrumentation.runOnMainSync { widgetView.getLocationOnScreen(location) }
            // CI displays can be narrower than a tested widget. Keep full-size layout assertions
            // above and export the part visible on the physical display.
            val bounds = android.graphics.Rect(location[0], location[1], location[0] + width, location[1] + height)
            assertTrue(bounds.intersect(0, 0, screen.width, screen.height))
            val bitmap = android.graphics.Bitmap.createBitmap(screen, bounds.left, bounds.top, bounds.width(), bounds.height())
            java.io.File(context.getExternalFilesDir(null), "$snapshot.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        result
    }
    private fun texts(view: View): List<String> = listOfNotNull(view.contentDescription?.toString()) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else if (view is TextView) listOf(view.text.toString()) else emptyList()
    @Test fun bothProvidersExposeRealPreviewLayoutsAndImages() {
        val manager=AppWidgetManager.getInstance(context)
        for (provider in listOf(DietReminderReceiver::class.java,ShoppingWidgetReceiver::class.java)) {
            val info=manager.getInstalledProvidersForPackage(context.packageName,null).find { it.provider == ComponentName(context,provider) }
            assertNotNull(info); assertTrue(info!!.previewImage!=0); assertTrue(info.previewLayout!=0); assertTrue(info.initialLayout!=0)
            assertNotNull(info.loadPreviewImage(context,context.resources.displayMetrics.densityDpi))
            instrumentation.runOnMainSync {
                for (layout in listOf(info.previewLayout, info.initialLayout)) {
                    val preview=android.widget.RemoteViews(context.packageName,layout).apply(context,FrameLayout(context))
                    assertTrue(texts(preview).isNotEmpty())
                }
            }
        }
    }
    @Test fun mealsRenderCourseNamesAcrossSizesAndThemes() {
        val now=LocalDateTime.of(2026,10,19,11,0)
        val meal=MealWithDetails(Meal(id=1,dietId=1,dayOfWeek=DayOfWeek.MONDAY,type=MealType.LUNCH,timeMinutes=780),listOf(CourseWithItems(Course(id=1,mealId=1,name="Pasta al pesto"),emptyList())))
        val model=mealWidgetModel(listOf(meal),now,0,"fixture")
        for(dark in listOf(true,false)) for(size in listOf(DpSize(180.dp,160.dp),DpSize(230.dp,220.dp),DpSize(280.dp,250.dp),DpSize(400.dp,320.dp))) {
            val text=render(size, "widget-meals-$dark-${size.width.value.toInt()}x${size.height.value.toInt()}") { MealWidgetContent(context,model,"Piano di test",brandColorScheme(dark),"fixture",now) }
            assertTrue(text.contains("Pasta al pesto"));assertFalse(text.contains("13:00"));assertTrue(text.contains(context.getString(R.string.meal_lunch)))
            if (size.width >= 240.dp) assertTrue(text.contains(context.getString(R.string.widget_next_default)))
        }
    }
    @Test fun emptyMealWidgetHasAnActionableLocalizedState() {
        val now=LocalDateTime.of(2026,10,19,11,0)
        val text=render(DpSize(180.dp,160.dp)) { MealWidgetContent(context,mealWidgetModel(emptyList(),now,0,"fixture"),"",brandColorScheme(true),"fixture",now) }
        assertTrue(text.contains(context.getString(R.string.widget_no_meals)))
        assertTrue(text.contains(context.getString(R.string.widget_open_app)))
    }
    @Test fun shoppingShowsEmptyStateAndRetainsErrorWithoutPretendingToSave() {
        val text=render(DpSize(280.dp,300.dp)) { ShoppingWidgetContent(context,true,null,emptyList(),emptyList(),brandColorScheme(true),"fixture",true) }
        assertTrue(text.contains(context.getString(R.string.widget_no_lists)))
        assertTrue(text.contains(context.getString(R.string.widget_write_failed)))
    }
    @Test fun shoppingRendersBothSizesAndThemes() {
        val list=ShoppingListWithItems(ShoppingList(id=1,name="Spesa di test"))
        val rows=listOf(ShoppingWidgetEntry(1,null,"Mele","2 pz","Frutta e verdura",1,false))
        for(dark in listOf(true,false)) for(size in listOf(DpSize(180.dp,160.dp),DpSize(230.dp,180.dp),DpSize(280.dp,300.dp),DpSize(400.dp,220.dp))) {
            val text=render(size, "widget-shopping-$dark-${size.width.value.toInt()}x${size.height.value.toInt()}") { ShoppingWidgetContent(context,true,list,rows,listOf(list),brandColorScheme(dark),"fixture",false) }
            assertTrue(text.contains("Mele"));assertTrue(text.contains("Spesa di test"));assertTrue(text.contains(context.getString(R.string.widget_shopping_remaining,1)))
        }
    }
    @Test fun shoppingShowsPurchaseControlsOnlyWhenWritesAreAllowed() {
        val oldOffline = UpdateManagerStatus.isOffline
        val oldChecking = UpdateManagerStatus.isChecking
        try {
            val list = ShoppingListWithItems(ShoppingList(id = 1, name = "Spesa"))
            val row = ShoppingWidgetEntry(1, null, "Mele", "2 pz", "Frutta e verdura", 1, false)
            for (blocked in listOf(false, true)) {
                UpdateManagerStatus.isChecking = false
                UpdateManagerStatus.isOffline = blocked
                val text = render(DpSize(280.dp, 300.dp)) {
                    ShoppingWidgetContent(context, true, list, listOf(row), listOf(list), brandColorScheme(true), "fixture", false)
                }
                assertEquals(!blocked, text.contains(context.getString(R.string.widget_mark_bought, "Mele")))
            }
        } finally {
            UpdateManagerStatus.isOffline = oldOffline
            UpdateManagerStatus.isChecking = oldChecking
        }
    }

}
