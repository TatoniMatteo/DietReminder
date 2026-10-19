package it.matato.dietreminder.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import it.matato.dietreminder.data.model.resId
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

internal object MealWidgetKeys {
    val id = longPreferencesKey("meal")
    val scope = stringPreferencesKey("scope")
    val date = stringPreferencesKey("date")
    val at = longPreferencesKey("selectedAt")
    val direction = ActionParameters.Key<Int>("direction")
    val expectedScope = ActionParameters.Key<String>("scope")
}
class DietReminder : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp, 160.dp), DpSize(280.dp, 250.dp)))
    companion object { suspend fun updateAll(context: Context) { DietReminder().updateAll(context) } }
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DietApplication
        val scope = app.currentAccountScope()
        val repos = app.accountRepositories.forScope(scope)
        val diet = repos.diets.active.first()
        val meals = diet?.let { repos.diets.getMeals(it.id) }.orEmpty()
        val prefs = getAppWidgetState<Preferences>(context, id)
        val selection = prefs[MealWidgetKeys.id]?.let { MealWidgetSelection(it, prefs[MealWidgetKeys.scope].orEmpty(), prefs[MealWidgetKeys.at] ?: 0L, prefs[MealWidgetKeys.date].orEmpty()) }
        val now = LocalDateTime.now()
        val model = mealWidgetModel(meals, now, diet?.nextMealWindowMinutes ?: 0, scope, selection)
        val scheme = widgetScheme(context, app)
        provideContent {
            val current = scope == app.currentAccountScope()
            val shown = model.takeIf { current } ?: MealWidgetModel(null, emptyList(), false)
            MealWidgetContent(context, shown, if (current) diet?.name ?: context.getString(R.string.no_diet) else context.getString(R.string.no_diet), scheme, scope, now)

        }
    }
}
@Composable
internal fun MealWidgetContent(context: Context, shown: MealWidgetModel, dietName: String, scheme: androidx.compose.material3.ColorScheme, scope: String, now: LocalDateTime) {
            val meal = shown.selected?.meal
            val open = widgetOpen(context, meal?.let { "week?mealId=${it.meal.id}&dayName=${it.meal.dayOfWeek.name}" } ?: "week")
            Column(GlanceModifier.widgetSurface(scheme)) {
                WidgetBrand(context, context.getString(if (shown.browsing) R.string.widget_today_meals else R.string.next_meal), dietName, scheme, open)
                Spacer(GlanceModifier.height(8.dp))
                Column(GlanceModifier.fillMaxWidth().defaultWeight().clickable(open)) {
                    Text(meal?.meal?.customTypeLabel?.takeIf { it.isNotBlank() } ?: meal?.meal?.type?.let { context.getString(it.resId) } ?: context.getString(R.string.widget_no_meals), style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorProvider(scheme.onSurface)), maxLines = 1)
                    shown.selected?.let { selected ->
                        val tomorrow = selected.dateTime.toLocalDate() != now.toLocalDate()
                        Text((if (tomorrow) context.getString(R.string.widget_tomorrow) + " · " else "") + "%02d:%02d".format(meal!!.meal.timeMinutes / 60, meal.meal.timeMinutes % 60), style = TextStyle(fontSize = 16.sp, color = ColorProvider(scheme.primary), fontWeight = FontWeight.Bold), maxLines = 1)
                    }
                    if (LocalSize.current.height >= 250.dp) {
                        Spacer(GlanceModifier.height(8.dp))
                        LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                            meal?.courses?.forEach { course ->
                                item { Text(course.course.name, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(scheme.onSurfaceVariant)), maxLines = 1) }
                                course.items.forEach { food -> item { Text("${food.name}  ${food.displayQuantity}", style = TextStyle(fontSize = 12.sp, color = ColorProvider(scheme.onSurfaceVariant)), maxLines = 1) } }
                            }
                        }
                    }
                }
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (shown.today.isNotEmpty()) {
                        WidgetAction(context.getString(R.string.widget_previous_meal), scheme, mealAction(-1, scope), R.drawable.widget_previous)
                        Spacer(GlanceModifier.defaultWeight())
                        WidgetAction(context.getString(R.string.widget_next_default), scheme, mealAction(0, scope))
                        Spacer(GlanceModifier.defaultWeight())
                        WidgetAction(context.getString(R.string.widget_next_meal), scheme, mealAction(1, scope), R.drawable.widget_next)
                    } else WidgetAction(context.getString(R.string.widget_open_app), scheme, open)
                }
            }
}

private fun mealAction(direction: Int, scope: String) = actionRunCallback<BrowseMealsAction>(actionParametersOf(MealWidgetKeys.direction to direction, MealWidgetKeys.expectedScope to scope))
class BrowseMealsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DietApplication
        val scope = app.currentAccountScope()
        if (parameters[MealWidgetKeys.expectedScope] != scope || scope == "locked") { DietReminder().update(context, glanceId); return }
        val direction = parameters[MealWidgetKeys.direction] ?: return
        if (direction !in -1..1) return
        val repo = app.accountRepositories.forScope(scope).diets
        val diet = repo.active.first()
        val meals = diet?.let { repo.getMeals(it.id) }.orEmpty()
        val now = LocalDateTime.now()
        updateAppWidgetState(context, glanceId) { prefs ->
            if (scope != app.currentAccountScope()) return@updateAppWidgetState
            val selection = prefs[MealWidgetKeys.id]?.let { MealWidgetSelection(it, prefs[MealWidgetKeys.scope].orEmpty(), prefs[MealWidgetKeys.at] ?: 0L, prefs[MealWidgetKeys.date].orEmpty()) }
            val selected = mealWidgetModel(meals, now, diet?.nextMealWindowMinutes ?: 0, scope, selection).adjacent(direction)
            if (direction == 0 || selected == null) prefs.clear() else {
                prefs[MealWidgetKeys.id] = selected.meal.id
                prefs[MealWidgetKeys.scope] = scope
                prefs[MealWidgetKeys.date] = now.toLocalDate().toString()
                prefs[MealWidgetKeys.at] = System.currentTimeMillis()
            }
        }
        DietReminder().update(context, glanceId)
    }
}
class DietReminderReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DietReminder() }
