package it.matato.dietreminder.ui.theme

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private const val THEME_ANIMATION_DURATION_MS = 700

private val THEME_ANIMATION_EASING: Easing = FastOutSlowInEasing

@Composable
fun DietTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val targetScheme = remember(
        darkTheme,
        dynamicColor,
        seedColor,
    ) {
        when {
            dynamicColor -> {
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }

            seedColor != null -> colorSchemeFromSeed(seedColor, darkTheme)

            darkTheme -> darkColorScheme()

            else -> lightColorScheme()
        }
    }

    val colorScheme = animateColorScheme(targetScheme)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@Composable
private fun animateColorScheme(
    target: ColorScheme,
): ColorScheme {
    val transition = updateTransition(
        targetState = target,
        label = "colorScheme",
    )

    val colorSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color> = {
        tween(
            durationMillis = THEME_ANIMATION_DURATION_MS,
            easing = THEME_ANIMATION_EASING,
        )
    }

    val primary by transition.animateColor(colorSpec, label = "primary") {
        it.primary
    }

    val onPrimary by transition.animateColor(colorSpec, label = "onPrimary") {
        it.onPrimary
    }

    val primaryContainer by transition.animateColor(colorSpec, label = "primaryContainer") {
        it.primaryContainer
    }

    val onPrimaryContainer by transition.animateColor(colorSpec, label = "onPrimaryContainer") {
        it.onPrimaryContainer
    }

    val inversePrimary by transition.animateColor(colorSpec, label = "inversePrimary") {
        it.inversePrimary
    }

    val secondary by transition.animateColor(colorSpec, label = "secondary") {
        it.secondary
    }

    val onSecondary by transition.animateColor(colorSpec, label = "onSecondary") {
        it.onSecondary
    }

    val secondaryContainer by transition.animateColor(colorSpec, label = "secondaryContainer") {
        it.secondaryContainer
    }

    val onSecondaryContainer by transition.animateColor(colorSpec, label = "onSecondaryContainer") {
        it.onSecondaryContainer
    }

    val tertiary by transition.animateColor(colorSpec, label = "tertiary") {
        it.tertiary
    }

    val onTertiary by transition.animateColor(colorSpec, label = "onTertiary") {
        it.onTertiary
    }

    val tertiaryContainer by transition.animateColor(colorSpec, label = "tertiaryContainer") {
        it.tertiaryContainer
    }

    val onTertiaryContainer by transition.animateColor(colorSpec, label = "onTertiaryContainer") {
        it.onTertiaryContainer
    }

    val error by transition.animateColor(colorSpec, label = "error") {
        it.error
    }

    val onError by transition.animateColor(colorSpec, label = "onError") {
        it.onError
    }

    val errorContainer by transition.animateColor(colorSpec, label = "errorContainer") {
        it.errorContainer
    }

    val onErrorContainer by transition.animateColor(colorSpec, label = "onErrorContainer") {
        it.onErrorContainer
    }

    val background by transition.animateColor(colorSpec, label = "background") {
        it.background
    }

    val onBackground by transition.animateColor(colorSpec, label = "onBackground") {
        it.onBackground
    }

    val surface by transition.animateColor(colorSpec, label = "surface") {
        it.surface
    }

    val onSurface by transition.animateColor(colorSpec, label = "onSurface") {
        it.onSurface
    }

    val surfaceVariant by transition.animateColor(colorSpec, label = "surfaceVariant") {
        it.surfaceVariant
    }

    val onSurfaceVariant by transition.animateColor(colorSpec, label = "onSurfaceVariant") {
        it.onSurfaceVariant
    }

    val surfaceTint by transition.animateColor(colorSpec, label = "surfaceTint") {
        it.surfaceTint
    }

    val inverseSurface by transition.animateColor(colorSpec, label = "inverseSurface") {
        it.inverseSurface
    }

    val inverseOnSurface by transition.animateColor(colorSpec, label = "inverseOnSurface") {
        it.inverseOnSurface
    }

    val surfaceContainerLowest by transition.animateColor(
        colorSpec,
        label = "surfaceContainerLowest",
    ) {
        it.surfaceContainerLowest
    }

    val surfaceContainerLow by transition.animateColor(
        colorSpec,
        label = "surfaceContainerLow",
    ) {
        it.surfaceContainerLow
    }

    val surfaceContainer by transition.animateColor(
        colorSpec,
        label = "surfaceContainer",
    ) {
        it.surfaceContainer
    }

    val surfaceContainerHigh by transition.animateColor(
        colorSpec,
        label = "surfaceContainerHigh",
    ) {
        it.surfaceContainerHigh
    }

    val surfaceContainerHighest by transition.animateColor(
        colorSpec,
        label = "surfaceContainerHighest",
    ) {
        it.surfaceContainerHighest
    }

    val outline by transition.animateColor(colorSpec, label = "outline") {
        it.outline
    }

    val outlineVariant by transition.animateColor(colorSpec, label = "outlineVariant") {
        it.outlineVariant
    }

    val scrim by transition.animateColor(colorSpec, label = "scrim") {
        it.scrim
    }

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
    )
}
