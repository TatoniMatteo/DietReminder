package it.matato.dietreminder.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun colorSchemeFromSeed(seed: Color, isDark: Boolean): ColorScheme {
	if (seed == Color(0xFF246A42)) return brandColorScheme(isDark)
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

/** Shared with web theme.css: neutral surfaces, apple green accents. */
fun brandColorScheme(dark: Boolean): ColorScheme = if (dark) darkColorScheme(
    primary = Color(0xFF91E7A6), onPrimary = Color(0xFF103B27),
    primaryContainer = Color(0xFF233E30), onPrimaryContainer = Color(0xFFCFF4D8),
    secondary = Color(0xFFB3CBB9), onSecondary = Color(0xFF20372A),
    secondaryContainer = Color(0xFF2B3D33), onSecondaryContainer = Color(0xFFD5EBDD),
    tertiary = Color(0xFFBDDACB), tertiaryContainer = Color(0xFF2B4038),
    background = Color(0xFF101116), onBackground = Color(0xFFE9E9F2),
    surface = Color(0xFF101116), onSurface = Color(0xFFE9E9F2),
    surfaceContainerLowest = Color(0xFF101116), surfaceContainerLow = Color(0xFF181A21),
    surfaceContainer = Color(0xFF181A21), surfaceContainerHigh = Color(0xFF20232D),
    surfaceContainerHighest = Color(0xFF252834), surfaceVariant = Color(0xFF20232D),
    onSurfaceVariant = Color(0xFFA1A3B4), outline = Color(0xFF72768B), outlineVariant = Color(0xFF2A2C38),
    surfaceTint = Color.Transparent,
) else lightColorScheme(
    primary = Color(0xFF246A42), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF2E2), onPrimaryContainer = Color(0xFF17472C),
    secondary = Color(0xFF557C5D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0EDDF), onSecondaryContainer = Color(0xFF20372A),
    tertiary = Color(0xFF3D7161), tertiaryContainer = Color(0xFFD5EEE3),
    background = Color(0xFFF7F8FC), onBackground = Color(0xFF282B3D),
    surface = Color(0xFFF7F8FC), onSurface = Color(0xFF282B3D),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFF0F1F7),
    surfaceContainerHighest = Color(0xFFE9EBF5), surfaceVariant = Color(0xFFF0F1F7),
    onSurfaceVariant = Color(0xFF6C7185), outline = Color(0xFF8C91A2), outlineVariant = Color(0xFFE6E8F0),
    surfaceTint = Color.Transparent,
)
