package com.phasma.ghostdetector.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phasma.ghostdetector.ui.theme.PhasmaDanger
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaSpectral
import kotlin.math.sin

/**
 * 5-bar EMF meter. `level` ∈ [0,1]. When `animated`, idle bars ripple slowly.
 */
@Composable
fun EmfMeter(
    level: Float,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val infinite = rememberInfiniteTransition(label = "emf")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "emf-t"
    )
    val barCount = 5
    val effectiveLevel = level.coerceIn(0f, 1f)
    Row(modifier = modifier) {
        for (i in 0 until barCount) {
            val activationThreshold = (i + 1) / barCount.toFloat()
            val active = effectiveLevel >= (i / barCount.toFloat())
            val flicker = if (animated && active) 0.85f + 0.15f * sin(t + i) else 1f
            val color = when {
                !active -> Color.White.copy(alpha = 0.10f)
                i < 2 -> PhasmaSpectral.copy(alpha = flicker)
                i < 4 -> PhasmaGlow.copy(alpha = flicker)
                else -> PhasmaDanger.copy(alpha = flicker)
            }
            val barFraction = when {
                !active -> 0.20f
                effectiveLevel >= activationThreshold -> 1f
                else -> ((effectiveLevel - i / barCount.toFloat()) * barCount).coerceIn(0.20f, 1f)
            }
            Canvas(modifier = Modifier.width(14.dp).fillMaxHeight()) {
                val h = size.height * barFraction
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h),
                    size = androidx.compose.ui.geometry.Size(size.width, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
            if (i != barCount - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

/** Slim horizontal version */
@Composable
fun EmfBarHorizontal(level: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(6.dp)) {
        // Track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        val frac = level.coerceIn(0f, 1f)
        val color = when {
            frac < 0.4f -> PhasmaSpectral
            frac < 0.75f -> PhasmaGlow
            else -> PhasmaDanger
        }
        drawRoundRect(
            color = color,
            size = androidx.compose.ui.geometry.Size(size.width * frac, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
    }
}
