package com.phasma.ghostdetector.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaPlasma
import com.phasma.ghostdetector.ui.theme.PhasmaSpectral
import kotlin.math.cos
import kotlin.math.sin

/** Cartoon-ish ghost silhouette with a glowing aura. `pulse` ∈ [0,1] drives the aura. */
@Composable
fun GhostEmblem(
    modifier: Modifier = Modifier,
    pulse: Float = 0f,
    glowColor: Color = PhasmaGlow
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.32f

        // Aura
        val auraR = r * (1.7f + 0.25f * pulse)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(cx, cy),
                radius = auraR
            ),
            radius = auraR,
            center = Offset(cx, cy)
        )

        // Ghost body — half circle head + wavy bottom
        val path = Path().apply {
            // Head: top arc
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    offset = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f)
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            // Wavy bottom (4 humps)
            val humpCount = 4
            val humpW = (r * 2f) / humpCount
            for (i in 0 until humpCount) {
                val startX = cx + r - i * humpW
                val midX = startX - humpW / 2f
                val endX = startX - humpW
                val baseY = cy + r * 0.05f
                // sway phase shifts the dip a touch
                val sway = sin(pulse * 6.28f + i) * 4f
                quadraticBezierTo(midX, baseY - r * 0.25f + sway, endX, baseY)
            }
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, Color(0xFFB9C4FF))
            )
        )
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(glowColor, PhasmaPlasma)
            ),
            style = Stroke(width = 3f)
        )

        // Eyes — hollow ovals
        val eyeR = r * 0.18f
        val eyeOffsetX = r * 0.35f
        val eyeY = cy - r * 0.15f
        drawCircle(Color(0xFF101225), eyeR, Offset(cx - eyeOffsetX, eyeY))
        drawCircle(Color(0xFF101225), eyeR, Offset(cx + eyeOffsetX, eyeY))
        // Glowing pupil
        val pupilOffset = sin(pulse * 6.28f) * eyeR * 0.3f
        drawCircle(PhasmaSpectral, eyeR * 0.45f, Offset(cx - eyeOffsetX + pupilOffset, eyeY))
        drawCircle(PhasmaSpectral, eyeR * 0.45f, Offset(cx + eyeOffsetX + pupilOffset, eyeY))

        // Small mouth — "o"
        drawCircle(
            color = Color(0xFF101225),
            radius = r * 0.10f,
            center = Offset(cx, cy + r * 0.18f)
        )

        // Orbiting particles
        val particleCount = 5
        for (i in 0 until particleCount) {
            val angle = (i / particleCount.toFloat()) * 6.28f + pulse * 6.28f
            val px = cx + cos(angle) * auraR * 0.95f
            val py = cy + sin(angle) * auraR * 0.95f
            drawCircle(
                color = glowColor.copy(alpha = 0.8f),
                radius = 4f,
                center = Offset(px, py)
            )
        }
    }
}
