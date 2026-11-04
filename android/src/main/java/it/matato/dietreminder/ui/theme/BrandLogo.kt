package it.matato.dietreminder.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import it.matato.dietreminder.R
import kotlin.math.cos
import kotlin.math.sin

internal val LocalBrandLogoRotation = staticCompositionLocalOf { 0f }

internal fun logoHueRotation(color: Color, themed: Boolean): Float {
    if (!themed) return 0f
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return if (hsv[1] == 0f) 0f else hsv[0] - 140f
}

/** Recolor the original gradients without replacing the artwork with a flat tint. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    val rotation = LocalBrandLogoRotation.current
    val filter = remember(rotation) {
        if (rotation == 0f) null else {
            val radians = Math.toRadians(rotation.toDouble())
            val c = cos(radians).toFloat()
            val s = sin(radians).toFloat()
            // Same luminance-preserving hue rotation as the CSS hue-rotate filter.
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
                .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
                .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )))
        }
    }
    Image(painterResource(R.drawable.brand_logo), contentDescription = null, modifier = modifier, colorFilter = filter)
}
