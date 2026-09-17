package it.matato.dietreminder.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun colorSchemeFromSeed(seed: Color, isDark: Boolean): ColorScheme {
    val argb = seed.toArgb()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)

    return if (isDark) {
        darkColorScheme(
            primary = seed,
            onPrimary = getContrastColor(seed),
            primaryContainer = shiftColor(seed, 0.3f, 0.3f),
            onPrimaryContainer = Color.White,
            secondary = shiftHue(seed, 15f),
            tertiary = shiftHue(seed, -15f),
            surface = Color(0xFF1C1B1F),
            onSurface = Color(0xFFE6E1E5),
            background = Color(0xFF1C1B1F),
            onBackground = Color(0xFFE6E1E5)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = getContrastColor(seed),
            primaryContainer = shiftColor(seed, 0.9f, 0.1f),
            onPrimaryContainer = shiftColor(seed, 0.2f, 0.5f),
            secondary = shiftHue(seed, 15f),
            tertiary = shiftHue(seed, -15f),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F)
        )
    }
}

private fun getContrastColor(color: Color): Color {
    val luminance = ColorUtils.calculateLuminance(color.toArgb())
    return if (luminance > 0.5) Color.Black else Color.White
}

private fun shiftColor(color: Color, lightness: Float, satMult: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = (hsl[1] * satMult).coerceIn(0f, 1f)
    hsl[2] = lightness.coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun shiftHue(color: Color, amount: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[0] = (hsl[0] + amount) % 360f
    if (hsl[0] < 0) hsl[0] += 360f
    return Color(ColorUtils.HSLToColor(hsl))
}
