package it.matato.dietreminder.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.*
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.state.*
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.R
import it.matato.dietreminder.util.AppLog
import it.matato.dietreminder.util.UpdateManagerStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale

internal object ShoppingWidgetKeys {
    val list = longPreferencesKey("list")
    val scope = stringPreferencesKey("scope")
    val error = booleanPreferencesKey("error")
    val expectedScope = ActionParameters.Key<String>("scope")
    val selectedList = ActionParameters.Key<Long>("list")
    val item = ActionParameters.Key<Long>("item")
    val day = ActionParameters.Key<Long>("day")
    val bought = ActionParameters.Key<Boolean>("bought")
}
class ShoppingWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact
    companion object { suspend fun updateAll(context: Context) { ShoppingWidget().updateAll(context) } }
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DietApplication
        WidgetWritePolicy.ensureInitialized(context)
        val scope = app.currentAccountScope()
        val repos = app.accountRepositories.forScope(scope)
        val lists = repos.shopping.allShoppingLists.first().sortedBy { it.list.id }
        val prefs = getAppWidgetState<Preferences>(context, id)
        val selected = selectWidgetList(lists, prefs[ShoppingWidgetKeys.list].takeIf { prefs[ShoppingWidgetKeys.scope] == scope }, repos.diets.active.first()?.id)
        val entries = selected?.let(::shoppingWidgetEntries).orEmpty()
        val scheme = widgetScheme(context, app)
        provideContent {
            val current = scope == app.currentAccountScope() && scope != "locked"
            val rows = if (current) entries else emptyList()
            ShoppingWidgetContent(context, current, selected, rows, lists, scheme, scope, prefs[ShoppingWidgetKeys.error] == true && prefs[ShoppingWidgetKeys.scope] == scope)

        }
    }
}
@androidx.compose.runtime.Composable
internal fun ShoppingWidgetContent(context: Context, current: Boolean, selected: it.matato.dietreminder.data.database.relation.ShoppingListWithItems?, rows: List<ShoppingWidgetEntry>, lists: List<it.matato.dietreminder.data.database.relation.ShoppingListWithItems>, scheme: androidx.compose.material3.ColorScheme, scope: String, failed: Boolean) {
            val open = widgetOpen(context, if (current && selected != null) "shopping/${selected.list.id}" else "shoppinglists")
            Column(GlanceModifier.widgetSurface(scheme)) {
                WidgetBrand(context, if (current) selected?.list?.name ?: context.getString(R.string.widget_shopping) else context.getString(R.string.widget_shopping), context.getString(R.string.widget_shopping_remaining, rows.count { !it.bought }), scheme, open)
                if (failed) Text(context.getString(R.string.widget_write_failed), style = TextStyle(fontSize = 11.sp, color = ColorProvider(scheme.error)))
                Spacer(GlanceModifier.height(8.dp))
                if (rows.isEmpty()) Box(GlanceModifier.defaultWeight().fillMaxWidth().clickable(open).padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    Text(context.getString(if (selected == null || !current) R.string.widget_no_lists else R.string.widget_empty_list), style = TextStyle(fontSize = 14.sp, color = ColorProvider(scheme.onSurfaceVariant)))
                } else LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    rows.forEach { entry ->
                        item(itemId = entry.dayId?.let { -it } ?: entry.itemId) {
                            Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                val action = actionRunCallback<ToggleShoppingAction>(actionParametersOf(ShoppingWidgetKeys.expectedScope to scope, ShoppingWidgetKeys.selectedList to selected!!.list.id, ShoppingWidgetKeys.item to entry.itemId, ShoppingWidgetKeys.day to (entry.dayId ?: 0), ShoppingWidgetKeys.bought to !entry.bought))
                                if (!UpdateManagerStatus.writesBlocked) WidgetAction(context.getString(if (entry.bought) R.string.widget_mark_pending else R.string.widget_mark_bought, entry.name), scheme, action, if (entry.bought) R.drawable.widget_checked else R.drawable.widget_unchecked, tonal = false)
                                Column(GlanceModifier.defaultWeight().padding(vertical = 3.dp).clickable(open)) {
                                    Text(entry.name, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ColorProvider(if (entry.bought) scheme.onSurfaceVariant else scheme.onSurface)), maxLines = 2)
                                    val day = entry.purchaseDay?.let { DayOfWeek.of(it).getDisplayName(DayTextStyle.SHORT, context.resources.configuration.locales[0] ?: Locale.ENGLISH) }
                                    Text(listOfNotNull(entry.quantity.takeIf { it.isNotBlank() }, entry.category.takeIf { LocalSize.current.height >= 220.dp }, day).joinToString(" · "), style = TextStyle(fontSize = 11.sp, color = ColorProvider(scheme.onSurfaceVariant)), maxLines = 1)
                                }
                            }
                        }
                    }
                }
                if (current && lists.size > 1) {
                    val index = lists.indexOfFirst { it.list.id == selected?.list?.id }
                    Spacer(GlanceModifier.height(8.dp))
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WidgetAction(context.getString(R.string.widget_previous_list), scheme, listAction(lists[Math.floorMod(index - 1, lists.size)].list.id, scope), R.drawable.widget_previous)
                        Spacer(GlanceModifier.defaultWeight())
                        WidgetAction(context.getString(R.string.widget_open_list), scheme, open, if (LocalSize.current.width < 240.dp) R.drawable.widget_open else null)
                        Spacer(GlanceModifier.defaultWeight())
                        WidgetAction(context.getString(R.string.widget_next_list), scheme, listAction(lists[(index + 1) % lists.size].list.id, scope), R.drawable.widget_next)
                    }
                }
            }
}

private fun listAction(id: Long, scope: String) = actionRunCallback<SelectShoppingListAction>(actionParametersOf(ShoppingWidgetKeys.selectedList to id, ShoppingWidgetKeys.expectedScope to scope))
class SelectShoppingListAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DietApplication
        WidgetWritePolicy.ensureInitialized(context)
        val scope = app.currentAccountScope()
        val list = parameters[ShoppingWidgetKeys.selectedList] ?: return
        if (parameters[ShoppingWidgetKeys.expectedScope] == scope && scope != "locked") updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ShoppingWidgetKeys.list] = list; prefs[ShoppingWidgetKeys.scope] = scope; prefs[ShoppingWidgetKeys.error] = false
        }
        ShoppingWidget().update(context, glanceId)
    }
}
class ToggleShoppingAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DietApplication
        WidgetWritePolicy.ensureInitialized(context)
        val scope = app.currentAccountScope()
        val expected = parameters[ShoppingWidgetKeys.expectedScope] ?: return
        val listId = parameters[ShoppingWidgetKeys.selectedList] ?: return
        val itemId = parameters[ShoppingWidgetKeys.item] ?: return
        val dayId = parameters[ShoppingWidgetKeys.day]?.takeIf { it != 0L }
        val bought = parameters[ShoppingWidgetKeys.bought] ?: return
        if (scope != expected || scope == "locked" || UpdateManagerStatus.writesBlocked) { ShoppingWidget().update(context, glanceId); return }
        // Pin the repository before suspending: an account switch can never redirect a write.
        val repository = app.accountRepositories.forScope(scope).shopping
        var failed = false
        try {
            applyWidgetPurchase(repository, listId, itemId, dayId, bought,
                isCurrent = { scope == app.currentAccountScope() }, writesBlocked = { UpdateManagerStatus.writesBlocked })

        } catch (ex: CancellationException) { throw ex }
        catch (ex: Exception) { failed = true; AppLog.e("Shopping widget: unable to save purchase", ex) }
        if (scope == app.currentAccountScope()) updateAppWidgetState(context, glanceId) { prefs -> prefs[ShoppingWidgetKeys.scope] = scope; prefs[ShoppingWidgetKeys.error] = failed }
        ShoppingWidget.updateAll(context)
    }
}
class ShoppingWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ShoppingWidget() }
