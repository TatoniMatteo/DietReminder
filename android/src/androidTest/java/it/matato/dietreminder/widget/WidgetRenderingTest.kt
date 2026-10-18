package it.matato.dietreminder.widget

import android.appwidget.AppWidgetManager
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
import androidx.test.platform.app.InstrumentationRegistry
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.*
import it.matato.dietreminder.data.database.relation.*
import it.matato.dietreminder.data.model.MealType
import it.matato.dietreminder.ui.theme.brandColorScheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WidgetRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private class Fixture(private val content: @Composable () -> Unit): GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) { provideContent { content() } }
    }
    private fun render(size: DpSize, content: @Composable () -> Unit): List<String> = runBlocking {
        val views=Fixture(content).compose(context,size=size)
        var result: List<String> = emptyList()
        instrumentation.runOnMainSync {
            val parent=FrameLayout(context)
            val view=views.apply(context,parent)
            val density=context.resources.displayMetrics.density
            view.measure(View.MeasureSpec.makeMeasureSpec((size.width.value*density).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((size.height.value*density).toInt(),View.MeasureSpec.EXACTLY))
            view.layout(0,0,view.measuredWidth,view.measuredHeight)
            assertTrue(view.measuredHeight>0)
            result=texts(view)
        }
        result
    }
    private fun texts(view: View): List<String> = if(view is ViewGroup) (0 until view.childCount).flatMap { texts(view.getChildAt(it)) } else if(view is TextView) listOf(view.text.toString()) else emptyList()
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
    @Test fun mealsRenderBothSizesAndThemesWithNavigationAndRealTime() {
        val now=LocalDateTime.of(2026,10,19,11,0)
        val meal=MealWithDetails(Meal(id=1,dietId=1,dayOfWeek=DayOfWeek.MONDAY,type=MealType.LUNCH,timeMinutes=780),emptyList())
        val model=mealWidgetModel(listOf(meal),now,0,"fixture")
        for(dark in listOf(true,false)) for(size in listOf(DpSize(180.dp,160.dp),DpSize(280.dp,250.dp))) {
            val text=render(size) { MealWidgetContent(context,model,"Piano di test",brandColorScheme(dark),"fixture",now) }
            assertTrue(text.contains("13:00"));assertTrue(text.contains(context.getString(R.string.meal_lunch)))
            assertTrue(text.contains(context.getString(R.string.widget_next_default)))
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
        for(dark in listOf(true,false)) for(size in listOf(DpSize(230.dp,180.dp),DpSize(280.dp,300.dp))) {
            val text=render(size) { ShoppingWidgetContent(context,true,list,rows,listOf(list),brandColorScheme(dark),"fixture",false) }
            assertTrue(text.contains("Spesa di test"));assertTrue(text.contains(context.getString(R.string.widget_shopping_remaining,1)))
        }
    }
}
