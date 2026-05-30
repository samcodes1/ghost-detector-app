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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * A flickering candle drawn entirely in Compose Canvas — no assets needed.
 * Conjures a séance / haunted vibe instead of a sci-fi EMF meter.
 */
@Composable
fun Candle(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "candle")
    val flicker by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "flicker"
    )
    val flicker2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
        label = "flicker2"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Candle body
        val candleW = w * 0.18f
        val candleH = h * 0.55f
        val candleTop = h * 0.42f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEFE5D6), Color(0xFFBBA88B))
            ),
            topLeft = Offset(cx - candleW / 2f, candleTop),
            size = Size(candleW, candleH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        // Wick
        drawLine(
            color = Color(0xFF1A1410),
            start = Offset(cx, candleTop - 2f),
            end = Offset(cx, candleTop - h * 0.04f),
            strokeWidth = 2f
        )

        // Flame
        val flameH = h * 0.20f + sin(flicker) * 4f + sin(flicker2 * 1.7f) * 2f
        val flameW = h * 0.07f + cos(flicker) * 1.5f
        val flameTop = candleTop - h * 0.04f - flameH
        val flameCenterX = cx + sin(flicker2 * 1.3f) * 1.5f
        val flamePath = Path().apply {
            moveTo(flameCenterX, flameTop)
            cubicTo(
                flameCenterX + flameW, flameTop + flameH * 0.4f,
                flameCenterX + flameW * 0.7f, flameTop + flameH * 0.95f,
                flameCenterX, flameTop + flameH
            )
            cubicTo(
                flameCenterX - flameW * 0.7f, flameTop + flameH * 0.95f,
                flameCenterX - flameW, flameTop + flameH * 0.4f,
                flameCenterX, flameTop
            )
            close()
        }
        // Outer warm glow (halo)
        val haloR = flameH * 1.8f + sin(flicker * 2f) * 6f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFB452).copy(alpha = 0.35f + 0.10f * sin(flicker)),
                    Color.Transparent
                ),
                center = Offset(flameCenterX, flameTop + flameH * 0.5f),
                radius = haloR
            ),
            radius = haloR,
            center = Offset(flameCenterX, flameTop + flameH * 0.5f)
        )
        // Outer flame (orange)
        drawPath(flamePath, color = Color(0xFFFF8A00).copy(alpha = 0.85f))
        // Inner flame (yellow)
        val inner = Path().apply {
            moveTo(flameCenterX, flameTop + flameH * 0.15f)
            cubicTo(
                flameCenterX + flameW * 0.6f, flameTop + flameH * 0.45f,
                flameCenterX + flameW * 0.4f, flameTop + flameH * 0.85f,
                flameCenterX, flameTop + flameH * 0.92f
            )
            cubicTo(
                flameCenterX - flameW * 0.4f, flameTop + flameH * 0.85f,
                flameCenterX - flameW * 0.6f, flameTop + flameH * 0.45f,
                flameCenterX, flameTop + flameH * 0.15f
            )
            close()
        }
        drawPath(inner, color = Color(0xFFFFE082))
        // Bright core
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = flameW * 0.35f,
            center = Offset(flameCenterX, flameTop + flameH * 0.55f)
        )
    }
}
