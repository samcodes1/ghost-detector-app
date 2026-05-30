package com.phasma.ghostdetector.ui.prank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phasma.ghostdetector.PhasmaApp
import com.phasma.ghostdetector.audio.Sfx
import com.phasma.ghostdetector.audio.SfxGroup
import com.phasma.ghostdetector.audio.SoundManager
import com.phasma.ghostdetector.data.AppSettings
import com.phasma.ghostdetector.R
import com.phasma.ghostdetector.ui.components.EmfBarHorizontal
import com.phasma.ghostdetector.ui.components.FogBackground
import com.phasma.ghostdetector.ui.components.JumpScareVideo
import com.phasma.ghostdetector.ui.theme.PhasmaBone
import com.phasma.ghostdetector.ui.theme.PhasmaDanger
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaInk
import com.phasma.ghostdetector.ui.theme.PhasmaVoid
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private enum class PrankPhase { PICK_TIMER, SCANNING, JUMP_SCARE, REVEAL }

private data class TimerOption(val label: String, val seconds: Int)

private val TIMER_OPTIONS = listOf(
    TimerOption("3 sec",    3),
    TimerOption("10 sec",  10),
    TimerOption("30 sec",  30),
    TimerOption("1 min",   60),
    TimerOption("3 min",  180),
)

@Composable
fun PrankScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val sound = remember { PhasmaApp.instance.sound }
    val settings = remember { AppSettings.get(context) }

    var phase by remember { mutableStateOf(PrankPhase.PICK_TIMER) }
    var timerSec by remember { mutableIntStateOf(10) }
    var remaining by remember { mutableIntStateOf(0) }

    // When we leave the screen, stop ambient
    DisposableEffect(Unit) {
        onDispose { sound.stopAmbient() }
    }

    when (phase) {
        PrankPhase.PICK_TIMER -> PickTimer(
            initialSeconds = timerSec,
            onExit = onExit,
            onStart = { secs ->
                timerSec = secs
                remaining = secs
                phase = PrankPhase.SCANNING
            }
        )
        PrankPhase.SCANNING -> ScanningPhase(
            sound = sound,
            initialSeconds = timerSec,
            onTick = { remaining = it },
            onComplete = { phase = PrankPhase.JUMP_SCARE }
        )
        PrankPhase.JUMP_SCARE -> JumpScarePhase(
            sound = sound,
            scareIntensity = settings.scareIntensity,
            vibrationsOn = settings.vibrationsOn,
            onDone = { phase = PrankPhase.REVEAL }
        )
        PrankPhase.REVEAL -> RevealPhase(
            timerSec = timerSec,
            onAgain = { phase = PrankPhase.PICK_TIMER },
            onExit = onExit
        )
    }
}

@Composable
private fun PickTimer(
    initialSeconds: Int,
    onExit: () -> Unit,
    onStart: (Int) -> Unit,
) {
    var selected by remember { mutableIntStateOf(initialSeconds) }

    Box(modifier = Modifier.fillMaxSize().background(PhasmaVoid)) {
        FogBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 12.dp),
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
                    "Prank Mode",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    color = PhasmaBone
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "How long until the ghost strikes?",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = PhasmaBone.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            // Timer options
            TIMER_OPTIONS.forEach { opt ->
                TimerRow(opt, selected == opt.seconds) { selected = opt.seconds }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = PhasmaInk.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhasmaGlow.copy(alpha = 0.4f))
            ) {
                Text(
                    "🤫  Pick a timer, then hand the phone to your friend.\nThe app will look like it's scanning for ghosts.\nWhen the timer runs out — gotcha.",
                    modifier = Modifier.padding(14.dp),
                    color = PhasmaBone.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onStart(selected) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PhasmaDanger),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "BEGIN THE PRANK",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = PhasmaBone
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimerRow(opt: TimerOption, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) PhasmaDanger.copy(alpha = 0.2f) else PhasmaInk.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) PhasmaDanger else PhasmaBone.copy(alpha = 0.2f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(opt.label, color = PhasmaBone, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            if (selected) Text("✓", color = PhasmaDanger, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ScanningPhase(
    sound: SoundManager,
    initialSeconds: Int,
    onTick: (Int) -> Unit,
    onComplete: () -> Unit,
) {
    var remaining by remember { mutableIntStateOf(initialSeconds) }
    var fakeEmf by remember { mutableStateOf(0f) }

    // Start ambient + drive countdown
    LaunchedEffect(initialSeconds) {
        sound.startAmbient()
        var elapsedMs = 0L
        var lastBeepSec = -1
        while (remaining > 0) {
            delay(100)
            elapsedMs += 100
            // Random walking EMF — drifts up as time runs down for tension
            val progress = 1f - (remaining.toFloat() / initialSeconds)
            val noise = Random.nextFloat() * 0.25f
            val target = 0.2f + progress * 0.6f + noise * (1f + progress)
            fakeEmf = (fakeEmf * 0.8f + target * 0.2f).coerceIn(0f, 1f)
            val newRemain = (initialSeconds - (elapsedMs / 1000).toInt()).coerceAtLeast(0)
            if (newRemain != remaining) {
                remaining = newRemain
                onTick(newRemain)
                // Beep on each second when tension is rising
                if (remaining <= 10 && remaining != lastBeepSec) {
                    sound.play(Sfx.EmfBeep, volumeMul = 0.4f)
                    lastBeepSec = remaining
                }
            }
            // Random whisper teaser
            if (Random.nextFloat() < 0.012f) {
                sound.playRandom(SfxGroup.Whispers, volumeMul = 0.35f)
            }
        }
        // brief pause then SCARE
        sound.stopAmbient()
        delay(180)
        onComplete()
    }

    Box(modifier = Modifier.fillMaxSize().background(PhasmaVoid)) {
        FogBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                "SCANNING ENVIRONMENT",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = PhasmaGlow,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hold device steady. Spectral analysis in progress.",
                fontSize = 12.sp,
                color = PhasmaBone.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(40.dp))

            // Fake radar — concentric pulses
            FakeRadar(intensity = fakeEmf)

            Spacer(Modifier.height(28.dp))

            // EMF reading
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PhasmaInk.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhasmaGlow.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "EMF",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = PhasmaBone,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${(fakeEmf * 100).toInt()} µT",
                            fontWeight = FontWeight.Black,
                            color = if (fakeEmf > 0.75f) PhasmaDanger else PhasmaGlow
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    EmfBarHorizontal(level = fakeEmf)
                }
            }

            Spacer(Modifier.weight(1f))
            // Timer hidden small — so victim doesn't see it about to fire
            Text(
                "T-$remaining s",
                color = PhasmaBone.copy(alpha = 0.18f),
                fontSize = 10.sp
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FakeRadar(intensity: Float) {
    val rt = rememberInfiniteTransition(label = "radar")
    val sweep by rt.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        // pulses (3 expanding rings)
        for (i in 0..2) {
            val t = ((sweep + i / 3f) % 1f)
            Box(
                modifier = Modifier
                    .size((180 + t * 80).dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        PhasmaGlow.copy(alpha = (1f - t) * (0.3f + intensity * 0.5f)),
                        CircleShape
                    )
            )
        }
        // Center dot
        Box(
            modifier = Modifier.size((40 + intensity * 30).dp).clip(CircleShape)
                .background(PhasmaDanger.copy(alpha = 0.4f + intensity * 0.5f))
        )
    }
}

@Composable
private fun JumpScarePhase(
    sound: SoundManager,
    scareIntensity: Int,
    vibrationsOn: Boolean,
    onDone: () -> Unit,
) {
    val flashRate = remember { 70L }
    var redFlash by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Layered audio impact — sting + scream + sub-bass low
        sound.play(Sfx.StingWhoosh, volumeMul = 1f)
        sound.playRandom(SfxGroup.Screams, volumeMul = 1f)
        if (scareIntensity >= 1) sound.play(Sfx.StingLow, volumeMul = 0.85f)
        if (scareIntensity >= 2 && vibrationsOn) sound.vibrateJumpScare()
        // Red flash strobe overlay for first ~1.5 s — gives the video extra punch
        val strobeMs = 1500L
        var elapsed = 0L
        while (elapsed < strobeMs) {
            delay(flashRate)
            redFlash = !redFlash
            elapsed += flashRate
        }
        redFlash = false
        // Hold the video another ~1.7 s after the strobe ends
        delay(1700L)
        onDone()
    }

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        // Looping ghost-nun close-up video — the "real spirit"
        JumpScareVideo(
            rawResId = R.raw.ghost_scare,
            modifier = Modifier.fillMaxSize(),
            loop = true,
        )
        // Red strobe flash on top for added shock
        if (redFlash) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(PhasmaDanger.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun RevealPhase(
    timerSec: Int,
    onAgain: () -> Unit,
    onExit: () -> Unit,
) {
    val rt = rememberInfiniteTransition(label = "reveal")
    val pulse by rt.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "rp"
    )

    Box(modifier = Modifier.fillMaxSize().background(PhasmaVoid)) {
        FogBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "👻",
                fontSize = (90 * pulse).sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "GOTCHA!",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                letterSpacing = 4.sp,
                color = PhasmaDanger
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hope your friend's heart is still working.",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = PhasmaBone.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onAgain,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(50)
            ) { Text("Prank again", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhasmaInk,
                    contentColor = PhasmaBone
                )
            ) { Text("Back to home") }
        }
    }
}
