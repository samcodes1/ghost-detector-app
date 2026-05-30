package com.phasma.ghostdetector.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

/**
 * Animated blood drips running from the top of the screen. Each drip moves
 * downward at its own pace and re-appears at the top after reaching its
 * maximum extent. Drawn in Compose Canvas — no assets.
 */
@Composable
fun BloodDrips(modifier: Modifier = Modifier, dripCount: Int = 8) {
    val rt = rememberInfiniteTransition(label = "drips")
    val t by rt.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    // Stable per-drip random parameters so they don't reshuffle on each recomposition
    val drips = remember {
        List(dripCount) { i ->
            DripParams(
                xFrac = (i * 137 % 100) / 100f,                   // pseudo-random distribution
                lengthFrac = 0.10f + (i * 47 % 100) / 100f * 0.45f, // 10–55% of screen
                widthDp = 3f + (i * 73 % 100) / 100f * 4f,        // 3–7 dp wide
                speed = 0.6f + (i * 53 % 100) / 100f * 0.8f,      // 0.6–1.4x speed
                offset = (i * 191 % 100) / 100f,
            )
        }
    }
    Canvas(modifier = modifier) {
        drips.forEach { d ->
            val animT = ((t * d.speed) + d.offset) % 1f
            val maxY = size.height * d.lengthFrac
            val currentY = maxY * animT
            val x = size.width * d.xFrac
            // The drip itself — vertical streak with a teardrop at the bottom
            val path = Path().apply {
                moveTo(x - d.widthDp / 2f, 0f)
                lineTo(x + d.widthDp / 2f, 0f)
                lineTo(x + d.widthDp / 2f * 0.7f, currentY * 0.9f)
                // Teardrop bulge
                quadraticBezierTo(
                    x + d.widthDp * 1.6f, currentY,
                    x, currentY + d.widthDp * 2.2f
                )
                quadraticBezierTo(
                    x - d.widthDp * 1.6f, currentY,
                    x - d.widthDp / 2f * 0.7f, currentY * 0.9f
                )
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B0000), Color(0xFFB80000)),
                    startY = 0f,
                    endY = currentY + d.widthDp * 2.2f
                )
            )
            // Drop that broke off (small circle further down)
            val dripDrop = animT * (1f + sin(animT * 6.28f) * 0.05f)
            val dropY = (currentY + maxY * 0.18f * dripDrop).coerceAtMost(size.height)
            if (dripDrop > 0.3f) {
                drawCircle(
                    color = Color(0xFF8B0000).copy(alpha = 1f - (dripDrop - 0.3f) * 1.5f),
                    radius = d.widthDp * 0.9f,
                    center = Offset(x, dropY)
                )
            }
        }
    }
}

private data class DripParams(
    val xFrac: Float,
    val lengthFrac: Float,
    val widthDp: Float,
    val speed: Float,
    val offset: Float,
)
