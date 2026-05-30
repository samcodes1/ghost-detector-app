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
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fullscreen scream face. Drawn in Compose so it works at any resolution without
 * shipping a giant raster asset. Eyes follow a subtle jitter so it looks alive.
 */
@Composable
fun ScreamFace(modifier: Modifier = Modifier) {
    val rt = rememberInfiniteTransition(label = "scream")
    val jitter by rt.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Restart),
        label = "j"
    )
    val pulse by rt.animateFloat(
        initialValue = 0.9f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse),
        label = "p"
    )

    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height

        // Deep red radial flash behind face
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8B0000).copy(alpha = 0.5f), Color.Black),
                center = Offset(W / 2f, H / 2f),
                radius = maxOf(W, H)
            ),
            radius = maxOf(W, H),
            center = Offset(W / 2f, H / 2f)
        )

        // Splatter "blood" specks
        val splats = 36
        for (i in 0 until splats) {
            val ax = (i * 17 + 3) % 100 / 100f
            val ay = (i * 29 + 11) % 100 / 100f
            val rad = 3f + (i % 5) * 4f
            drawCircle(
                color = Color(0xFFA00000).copy(alpha = 0.55f),
                radius = rad,
                center = Offset(ax * W, ay * H),
            )
        }

        // Face = giant pale oval
        val faceW = W * 0.7f
        val faceH = H * 0.6f
        val faceCx = W / 2f
        val faceCy = H * 0.46f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC9BAB0), Color(0xFF1A0F0C)),
                center = Offset(faceCx, faceCy),
                radius = faceW * 0.7f
            ),
            topLeft = Offset(faceCx - faceW / 2f, faceCy - faceH / 2f),
            size = Size(faceW, faceH)
        )

        // Eyes — hollow black holes with red glow inside
        val eyeOffsetX = faceW * 0.18f
        val eyeY = faceCy - faceH * 0.10f
        val eyeR = faceW * 0.10f * pulse
        val jitterX = cos(jitter * 3f) * 2f
        val jitterY = sin(jitter * 5f) * 2f
        for (side in listOf(-1, 1)) {
            val ex = faceCx + side * eyeOffsetX + jitterX
            val ey = eyeY + jitterY
            // Black socket
            drawCircle(Color.Black, radius = eyeR, center = Offset(ex, ey))
            // Red inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF1A1A), Color.Transparent),
                    center = Offset(ex, ey),
                    radius = eyeR * 1.2f
                ),
                radius = eyeR * 1.2f,
                center = Offset(ex, ey)
            )
            // Tiny white pinpoint pupil — uncanny
            drawCircle(Color(0xFFEEEEEE), radius = eyeR * 0.18f, center = Offset(ex, ey))
        }

        // Cuts/scratches across the face
        val gashPath = Path().apply {
            moveTo(faceCx - faceW * 0.32f, faceCy - faceH * 0.30f)
            lineTo(faceCx + faceW * 0.28f, faceCy + faceH * 0.05f)
        }
        drawPath(gashPath, color = Color(0xFF6E0000), style = Stroke(width = 5f))
        val gashPath2 = Path().apply {
            moveTo(faceCx + faceW * 0.20f, faceCy - faceH * 0.35f)
            lineTo(faceCx - faceW * 0.05f, faceCy + faceH * 0.15f)
        }
        drawPath(gashPath2, color = Color(0xFF6E0000), style = Stroke(width = 4f))

        // Screaming mouth — huge open void with teeth
        val mouthW = faceW * 0.42f
        val mouthH = faceH * 0.34f
        val mouthCx = faceCx
        val mouthCy = faceCy + faceH * 0.22f
        drawOval(
            color = Color(0xFF050000),
            topLeft = Offset(mouthCx - mouthW / 2f, mouthCy - mouthH / 2f),
            size = Size(mouthW, mouthH)
        )
        // Teeth — jagged top + bottom
        val teethCount = 7
        val teethStep = mouthW / teethCount
        for (t in 0 until teethCount) {
            val x = mouthCx - mouthW / 2f + t * teethStep
            val toothTop = Path().apply {
                moveTo(x, mouthCy - mouthH / 2f)
                lineTo(x + teethStep / 2f, mouthCy - mouthH / 2f + teethStep * 0.7f)
                lineTo(x + teethStep, mouthCy - mouthH / 2f)
            }
            drawPath(toothTop, color = Color(0xFFD8C9B5))
            val toothBot = Path().apply {
                moveTo(x, mouthCy + mouthH / 2f)
                lineTo(x + teethStep / 2f, mouthCy + mouthH / 2f - teethStep * 0.6f)
                lineTo(x + teethStep, mouthCy + mouthH / 2f)
            }
            drawPath(toothBot, color = Color(0xFFCCBC9F))
        }

        // Final vignette
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black),
                center = Offset(W / 2f, H / 2f),
                radius = maxOf(W, H) * 0.8f
            ),
            radius = maxOf(W, H) * 0.8f,
            center = Offset(W / 2f, H / 2f)
        )
    }
}
