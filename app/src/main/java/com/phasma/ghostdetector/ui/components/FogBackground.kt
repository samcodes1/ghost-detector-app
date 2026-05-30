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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaInk
import com.phasma.ghostdetector.ui.theme.PhasmaPlasma
import com.phasma.ghostdetector.ui.theme.PhasmaVoid
import kotlin.math.sin
import kotlin.random.Random

/** Animated, looping mist + star-field background — used on splash & home. */
@Composable
fun FogBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fog")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val stars = remember {
        List(60) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 1.6f + 0.3f)
        }
    }

    Canvas(modifier = modifier) {
        // Base radial vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(PhasmaInk, PhasmaVoid),
                center = Offset(size.width * 0.5f, size.height * 0.35f),
                radius = size.maxDimension
            )
        )

        // Two drifting fog blobs
        val w = size.width
        val h = size.height
        val fogAlpha = 0.18f + 0.06f * sin(pulse)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PhasmaPlasma.copy(alpha = fogAlpha), Color.Transparent),
                center = Offset(w * (0.2f + drift * 0.6f), h * 0.7f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * (0.2f + drift * 0.6f), h * 0.7f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PhasmaGlow.copy(alpha = fogAlpha * 0.6f), Color.Transparent),
                center = Offset(w * (1f - drift) * 0.9f, h * 0.25f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * (1f - drift) * 0.9f, h * 0.25f)
        )

        // Stars
        stars.forEach { (sx, sy, sr) ->
            val twinkle = 0.5f + 0.5f * sin(pulse + sx * 6f)
            drawCircle(
                color = Color.White.copy(alpha = 0.35f * twinkle),
                radius = sr,
                center = Offset(sx * w, sy * h)
            )
        }
    }
}
