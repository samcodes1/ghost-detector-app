package com.phasma.ghostdetector.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.phasma.ghostdetector.data.AppSettings
import com.phasma.ghostdetector.ui.components.FogBackground
import com.phasma.ghostdetector.ui.theme.PhasmaBone
import com.phasma.ghostdetector.ui.theme.PhasmaDanger
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaPlasma
import com.phasma.ghostdetector.ui.theme.PhasmaSpectral
import com.phasma.ghostdetector.ui.theme.PhasmaVoid

private data class OnboardPage(
    val icon: String,
    val title: String,
    val body: String,
    val accent: androidx.compose.ui.graphics.Color,
)

private val PAGES = listOf(
    OnboardPage(
        "👻", "Welcome to Ghost Finder",
        "An AR app that lets you scan rooms for ghosts and prank your friends. " +
            "Best with headphones in a dim room.",
        PhasmaGlow
    ),
    OnboardPage(
        "🔍", "Three ways to play",
        "GHOST HUNT — point at a doorway, watch what appears.\n" +
            "PRANK MODE — set a timer, hand the phone over, wait for the scream.\n" +
            "SPIRIT BOX — radio static + whispered words from the other side.",
        PhasmaPlasma
    ),
    OnboardPage(
        "🤫", "House rules",
        "Don't prank anyone with a weak heart, while driving, or in public " +
            "where it'll startle bystanders. Keep it fun.",
        PhasmaDanger
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    var page by remember { mutableIntStateOf(0) }
    val current = PAGES[page]

    Box(modifier = Modifier.fillMaxSize().background(PhasmaVoid)) {
        FogBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    settings.onboardingDone = true
                    onFinished()
                }) {
                    Text("Skip", color = PhasmaBone.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.weight(0.6f))

            // Icon
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(current.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(current.icon, fontSize = 80.sp)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                current.title,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                fontSize = 28.sp,
                color = PhasmaBone,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                current.body,
                color = PhasmaBone.copy(alpha = 0.75f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PAGES.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(
                            if (i == page) PhasmaSpectral else PhasmaBone.copy(alpha = 0.25f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (page < PAGES.size - 1) {
                        page++
                    } else {
                        settings.onboardingDone = true
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = current.accent, contentColor = PhasmaVoid)
            ) {
                Text(
                    if (page < PAGES.size - 1) "Next" else "Enter the dark",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
