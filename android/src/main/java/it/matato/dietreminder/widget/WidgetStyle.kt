package it.matato.dietreminder.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import it.matato.dietreminder.DietApplication
import it.matato.dietreminder.MainActivity
import it.matato.dietreminder.R
import it.matato.dietreminder.data.database.entity.ConfigKey
import it.matato.dietreminder.ui.theme.brandColorScheme
import it.matato.dietreminder.ui.theme.colorSchemeFromSeed
import kotlinx.coroutines.flow.first

internal suspend fun widgetScheme(context: Context, app: DietApplication): ColorScheme {
    val config = app.accountRepositories.current.config
    val theme = config.observeConfig(ConfigKey.THEME).first()?.value
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES }
    if (config.observeConfig(ConfigKey.USE_DYNAMIC_COLORS).first()?.value == "true") return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val seed = config.observeConfig(ConfigKey.SEED_COLOR).first()?.value
    return seed?.let { runCatching { colorSchemeFromSeed(if (it.startsWith("0x")) Color(it.removePrefix("0x").toLong(16)) else Color(android.graphics.Color.parseColor(it)), dark) }.getOrNull() } ?: brandColorScheme(dark)
}
internal fun widgetOpen(context: Context, path: String): Action = actionStartActivity(Intent(Intent.ACTION_VIEW, "dietreminder://$path".toUri(), context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))

@Composable
internal fun WidgetAction(label: String, scheme: ColorScheme, action: Action, icon: Int? = null) {
    Box(GlanceModifier.height(48.dp).let { if (icon == null) it.padding(horizontal = 8.dp) else it.width(48.dp) }.clickable(action), contentAlignment = Alignment.Center) {
        if (icon == null) Text(label, style = TextStyle(color = ColorProvider(scheme.primary), fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        else Image(ImageProvider(icon), label, GlanceModifier.size(22.dp), colorFilter = ColorFilter.tint(ColorProvider(scheme.primary)))
    }
}
@Composable
internal fun WidgetBrand(context: Context, title: String, subtitle: String, scheme: ColorScheme, open: Action) {
    Row(GlanceModifier.fillMaxWidth().clickable(open), verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(R.drawable.widget_logo), null, GlanceModifier.size(30.dp))
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(title, style = TextStyle(color = ColorProvider(scheme.onSurface), fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(subtitle, style = TextStyle(color = ColorProvider(scheme.onSurfaceVariant), fontSize = 11.sp), maxLines = 1)
        }
        Image(ImageProvider(R.drawable.widget_open), context.getString(R.string.widget_open_app), GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(ColorProvider(scheme.onSurfaceVariant)))
    }
}
internal fun GlanceModifier.widgetSurface(scheme: ColorScheme): GlanceModifier = fillMaxSize().background(scheme.surfaceContainerLow).cornerRadius(24.dp).padding(14.dp)
