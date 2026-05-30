package com.phasma.ghostdetector.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phasma.ghostdetector.ui.components.FogBackground
import com.phasma.ghostdetector.ui.components.GhostEmblem
import com.phasma.ghostdetector.ui.theme.PhasmaBone
import com.phasma.ghostdetector.ui.theme.PhasmaVoid
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(2600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhasmaVoid),
        contentAlignment = Alignment.Center
    ) {
        FogBackground(modifier = Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GhostEmblem(
                modifier = Modifier.size(200.dp),
                pulse = pulse
            )
            Text(
                text = "Ghost Detector",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                fontSize = 44.sp,
                letterSpacing = 2.sp,
                color = PhasmaBone,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "they are already here",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "listening…",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        )
    }
}
