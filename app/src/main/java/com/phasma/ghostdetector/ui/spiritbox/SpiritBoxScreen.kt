package com.phasma.ghostdetector.ui.spiritbox

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phasma.ghostdetector.PhasmaApp
import com.phasma.ghostdetector.audio.SfxGroup
import com.phasma.ghostdetector.ui.components.FogBackground
import com.phasma.ghostdetector.ui.theme.PhasmaBone
import com.phasma.ghostdetector.ui.theme.PhasmaDanger
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaInk
import com.phasma.ghostdetector.ui.theme.PhasmaPlasma
import com.phasma.ghostdetector.ui.theme.PhasmaVoid
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private val WORD_BANK = listOf(
    "behind", "you", "leave", "now", "help", "cold",
    "watching", "soon", "mine", "hurt", "run", "still",
    "here", "wake", "she", "name", "father", "home",
    "down", "door", "blood", "no", "stop", "alone",
    "open", "child", "die", "promise", "find", "look",
)

@Composable
fun SpiritBoxScreen(onExit: () -> Unit) {
    val sound = remember { PhasmaApp.instance.sound }

    var listening by remember { mutableStateOf(false) }
    var frequency by remember { mutableIntStateOf(88) }    // shown 88..108 MHz
    val transcribed = remember { mutableStateListOf<String>() }

    DisposableEffect(Unit) {
        onDispose { sound.stopRadioStatic() }
    }

    LaunchedEffect(listening) {
        if (!listening) {
            sound.stopRadioStatic()
            return@LaunchedEffect
        }
        sound.startRadioStatic()
        while (listening) {
            // Sweep frequency randomly to simulate a real spirit-box scan
            frequency = (88 + Random.nextInt(0, 20))
            // Random chance to "hear" a voice (whisper) and "transcribe" a word
            if (Random.nextFloat() < 0.18f) {
                sound.playRandom(SfxGroup.Whispers, volumeMul = 0.85f)
                val word = WORD_BANK.random()
                transcribed.add(0, word.uppercase())
                if (transcribed.size > 24) transcribed.removeAt(transcribed.lastIndex)
            }
            delay(380 + Random.nextLong(0, 220))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PhasmaVoid)) {
        FogBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = PhasmaBone)
                }
                Spacer(Modifier.fillMaxWidth(0.05f))
                Text(
                    "Spirit Box",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    color = PhasmaBone
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Sweeping radio frequencies — spirits may speak through the static.",
                color = PhasmaBone.copy(alpha = 0.65f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // Frequency display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PhasmaInk),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhasmaGlow.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$frequency.${Random.nextInt(0, 10)}  MHz",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (listening) PhasmaGlow else PhasmaBone.copy(alpha = 0.4f),
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    StaticWaveform(active = listening)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Transcribed words flow
            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                colors = CardDefaults.cardColors(containerColor = PhasmaInk.copy(alpha = 0.78f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhasmaPlasma.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "TRANSCRIBED  VOICES",
                        color = PhasmaPlasma,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    if (transcribed.isEmpty()) {
                        Text(
                            if (listening) "…listening…" else "Press the button to start scanning.",
                            color = PhasmaBone.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    } else {
                        transcribed.take(8).forEachIndexed { i, w ->
                            val alpha = 1f - (i * 0.10f)
                            Text(
                                "› $w",
                                color = if (i == 0) PhasmaDanger else PhasmaBone.copy(alpha = alpha.coerceAtLeast(0.3f)),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (i == 0) FontWeight.Black else FontWeight.Normal,
                                fontSize = if (i == 0) 18.sp else 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Big toggle
            ScanToggleButton(
                listening = listening,
                onClick = {
                    listening = !listening
                    if (!listening) transcribed.clear()
                }
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StaticWaveform(active: Boolean) {
    val rt = rememberInfiniteTransition(label = "wave")
    val t by rt.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "wt"
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        val bars = 28
        for (i in 0 until bars) {
            val phase = (i * 0.4f) + t * 6.28f
            val amp = if (active) (sin(phase) * 0.5f + 0.5f) * h * 0.8f else h * 0.05f
            val x = (i + 0.5f) * (w / bars)
            drawLine(
                color = if (active) PhasmaGlow else PhasmaBone.copy(alpha = 0.2f),
                start = Offset(x, mid - amp / 2f),
                end = Offset(x, mid + amp / 2f),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
private fun ScanToggleButton(listening: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (listening) PhasmaDanger else PhasmaGlow,
            contentColor = PhasmaVoid
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(12.dp).clip(CircleShape)
                    .background(if (listening) Color.White else PhasmaVoid)
                    .border(2.dp, PhasmaVoid, CircleShape)
            )
            Spacer(Modifier.fillMaxWidth(0.04f))
            Text(
                if (listening) "STOP SCANNING" else "START SCANNING",
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
        }
    }
}
