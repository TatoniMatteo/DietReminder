package it.matato.dietreminder.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Shared rhythm for screens, sections and the contents of a card. */
val LocalCompactLayout = compositionLocalOf { false }

object AppSpace {
    val screen @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 12.dp else 24.dp
    val section @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 12.dp else 24.dp
    val card @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 10.dp else 20.dp
    val group @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 8.dp else 16.dp
    val item @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 6.dp else 12.dp
    val row @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 2.dp else 8.dp
    val iconPadding @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 8.dp else 12.dp
    val inline @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 6.dp else 8.dp
    val small @Composable @ReadOnlyComposable get() = if (LocalCompactLayout.current) 2.dp else 4.dp
}

object AppMotion {
    const val quick = 160
    const val standard = 260
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
