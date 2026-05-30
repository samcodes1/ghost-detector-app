package com.phasma.ghostdetector.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phasma.ghostdetector.R
import com.phasma.ghostdetector.ads.BannerAd
import com.phasma.ghostdetector.ui.components.BloodDrips
import com.phasma.ghostdetector.ui.components.FogBackground
import com.phasma.ghostdetector.ui.components.JumpScareVideo
import com.phasma.ghostdetector.ui.theme.PhasmaBone
import com.phasma.ghostdetector.ui.theme.PhasmaDanger
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaInk
import com.phasma.ghostdetector.ui.theme.PhasmaPlasma
import com.phasma.ghostdetector.ui.theme.PhasmaSpectral
import com.phasma.ghostdetector.ui.theme.PhasmaVoid
import kotlinx.coroutines.delay
import kotlin.math.sin

private val WHISPERS = listOf(
    "There's a draft no one feels.",
    "The mirror just blinked.",
    "It's standing behind the door.",
    "Don't turn off the light.",
    "It wasn't your reflection.",
    "Whose breath is on your neck?",
    "Something moved on the stairs.",
    "Don't look behind you.",
)

@Composable
fun HomeScreen(
    onStartHunt: () -> Unit,
    onStartPrank: () -> Unit,
    onStartSpiritBox: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "home")
    val breath by transition.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val slowBreath by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse),
        label = "slow"
    )

    var whisperIdx by remember { mutableIntStateOf(0) }
    val whisperAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5500)
            whisperAlpha.animateTo(0f, tween(550, easing = LinearEasing))
            whisperIdx = (whisperIdx + 1) % WHISPERS.size
            whisperAlpha.animateTo(1f, tween(800, easing = LinearEasing))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ── Layer 1: looping scare video, heavily dimmed
        JumpScareVideo(
            rawResId = R.raw.ghost_scare,
            modifier = Modifier.fillMaxSize().alpha(0.22f),
            loop = true,
        )
        // ── Layer 2: dark vignette so the nun is barely visible
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        PhasmaVoid.copy(alpha = 0.55f),
                        PhasmaVoid.copy(alpha = 0.95f),
                    )
                )
            )
        )
        // ── Layer 3: drifting fog
        FogBackground(modifier = Modifier.fillMaxSize().alpha(0.5f))
        // ── Layer 4: blood drips from the top
        BloodDrips(modifier = Modifier.fillMaxSize().alpha(0.85f))

        // ── Layer 5: content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row: settings only
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = PhasmaBone.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // PULSING RED ALERT BANNER
            AlertBanner(pulse = breath)

            Spacer(Modifier.height(16.dp))

            // BIG TITLE — distressed horror style (serif + uppercase + tracked + red drop)
            Text(
                "GHOST",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 56.sp,
                letterSpacing = 6.sp,
                color = PhasmaBone,
            )
            Text(
                "DETECTOR",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,            // smaller than "GHOST" because DETECTOR is wider
                letterSpacing = 4.sp,
                color = PhasmaDanger.copy(alpha = 0.65f + 0.35f * breath),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "— it sees you back —",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = PhasmaBone.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            // Whisper of the moment — italic, ominous
            Text(
                "“${WHISPERS[whisperIdx]}”",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                color = PhasmaBone.copy(alpha = 0.85f * whisperAlpha.value),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().alpha(whisperAlpha.value)
            )

            Spacer(Modifier.height(20.dp))

            // Primary CTA: PRANK MODE (loud, pulsing, screams for attention)
            PrankCTA(pulse = breath, onClick = onStartPrank)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallTile(
                    title = "GHOST HUNT",
                    subtitle = "AR scan",
                    icon = "🔍",
                    accent = PhasmaGlow,
                    onClick = onStartHunt,
                    modifier = Modifier.weight(1f)
                )
                SmallTile(
                    title = "SPIRIT BOX",
                    subtitle = "EVP audio",
                    icon = "📻",
                    accent = PhasmaPlasma,
                    onClick = onStartSpiritBox,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))

            // Foot hint with slow breathing
            Text(
                "— turn off the lights. plug in headphones. then dare. —",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                color = PhasmaBone.copy(alpha = 0.35f + 0.25f * slowBreath),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            // AdMob banner — sits above the system gesture area thanks to the
            // outer windowInsetsPadding on the parent Column.
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AlertBanner(pulse: Float) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(PhasmaDanger.copy(alpha = 0.18f + 0.20f * pulse))
            .border(1.dp, PhasmaDanger.copy(alpha = 0.45f + 0.35f * pulse), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(PhasmaDanger.copy(alpha = pulse))
        )
        Spacer(Modifier.size(8.dp))
        Text(
            "PARANORMAL ACTIVITY DETECTED",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = PhasmaBone
        )
    }
}

@Composable
private fun PrankCTA(pulse: Float, onClick: () -> Unit) {
    val haloAlpha = 0.25f + 0.30f * pulse
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4A0010),
                        Color(0xFF1A0008),
                        Color(0xFF400010),
                    )
                )
            )
            .border(
                2.dp,
                PhasmaDanger.copy(alpha = 0.55f + 0.35f * pulse),
                RoundedCornerShape(18.dp)
            )
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
    ) {
        // Pulsing red glow on the left
        Box(
            modifier = Modifier
                .padding(start = 18.dp)
                .align(Alignment.CenterStart)
                .size(74.dp)
                .clip(CircleShape)
                .background(PhasmaDanger.copy(alpha = haloAlpha))
                .border(2.dp, PhasmaDanger, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("👻", fontSize = 38.sp)
        }
        Column(
            modifier = Modifier
                .padding(start = 110.dp, end = 18.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "PRANK MODE",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 4.sp,
                color = PhasmaBone,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Set a timer. Hand the phone over.\nWatch them scream.",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = PhasmaBone.copy(alpha = 0.75f),
            )
        }
        Text(
            "›",
            color = PhasmaDanger,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
        )
    }
}

@Composable
private fun SmallTile(
    title: String,
    subtitle: String,
    icon: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(PhasmaInk.copy(alpha = 0.92f), accent.copy(alpha = 0.12f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(icon, fontSize = 26.sp)
            Column {
                Text(
                    title,
                    color = PhasmaBone,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                )
                Text(
                    subtitle,
                    color = PhasmaBone.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
