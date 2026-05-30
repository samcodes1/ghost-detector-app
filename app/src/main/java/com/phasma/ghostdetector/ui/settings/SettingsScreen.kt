package com.phasma.ghostdetector.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phasma.ghostdetector.PhasmaApp
import com.phasma.ghostdetector.data.AppSettings
import com.phasma.ghostdetector.ui.components.FogBackground
import com.phasma.ghostdetector.ui.theme.PhasmaBone
import com.phasma.ghostdetector.ui.theme.PhasmaDanger
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaInk
import com.phasma.ghostdetector.ui.theme.PhasmaPlasma
import com.phasma.ghostdetector.ui.theme.PhasmaSpectral
import com.phasma.ghostdetector.ui.theme.PhasmaVoid

@Composable
fun SettingsScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val sound = remember { PhasmaApp.instance.sound }

    // Sync sound manager whenever settings change
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(settings.muted, settings.sfxVolume, settings.ambientVolume) }
            .collect { (muted, sfxV, ambV) ->
                sound.muted = muted
                sound.sfxVolume = sfxV
                sound.ambientVolume = ambV
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(PhasmaVoid)) {
        FogBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = PhasmaBone)
                }
                Spacer(Modifier.fillMaxWidth(0.04f))
                Text(
                    "Settings",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    color = PhasmaBone
                )
            }

            Spacer(Modifier.height(20.dp))

            SettingsSection("Audio") {
                ToggleRow(
                    title = "Mute everything",
                    subtitle = "Silence all sounds across the app",
                    checked = settings.muted,
                    onChange = { settings.muted = it }
                )
                Spacer(Modifier.height(8.dp))
                SliderRow(
                    title = "SFX volume",
                    value = settings.sfxVolume,
                    enabled = !settings.muted,
                    onChange = { settings.sfxVolume = it }
                )
                Spacer(Modifier.height(8.dp))
                SliderRow(
                    title = "Ambient volume",
                    value = settings.ambientVolume,
                    enabled = !settings.muted,
                    onChange = { settings.ambientVolume = it }
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsSection("Pranks") {
                ToggleRow(
                    title = "Vibration",
                    subtitle = "Jump scares shake the phone",
                    checked = settings.vibrationsOn,
                    onChange = { settings.vibrationsOn = it }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Scare intensity",
                    color = PhasmaBone,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                IntensitySegments(
                    selected = settings.scareIntensity,
                    onChange = { settings.scareIntensity = it }
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsSection("About") {
                Text(
                    "Ghost Detector · v1.0",
                    color = PhasmaBone.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "3D model: \"Dark Faceless Black Assassin\" by Pigcraft, CC-BY 4.0 (Sketchfab)\n" +
                        "Sound effects: Mixkit license — free for commercial use",
                    color = PhasmaBone.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(),
        color = PhasmaGlow,
        fontWeight = FontWeight.Black,
        letterSpacing = 3.sp,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PhasmaInk.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PhasmaGlow.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PhasmaBone, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (subtitle != null) {
                Text(subtitle, color = PhasmaBone.copy(alpha = 0.55f), fontSize = 11.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PhasmaBone,
                checkedTrackColor = PhasmaSpectral,
                uncheckedThumbColor = PhasmaBone.copy(alpha = 0.5f),
                uncheckedTrackColor = PhasmaInk
            )
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = if (enabled) PhasmaBone else PhasmaBone.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${(value * 100).toInt()}%",
                color = if (enabled) PhasmaGlow else PhasmaBone.copy(alpha = 0.3f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = PhasmaSpectral,
                activeTrackColor = PhasmaSpectral,
                inactiveTrackColor = PhasmaInk
            )
        )
    }
}

@Composable
private fun IntensitySegments(selected: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Mild", "Medium", "Insane").forEachIndexed { i, label ->
            val isSel = selected == i
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onChange(i) }
                    .background(
                        if (isSel) PhasmaDanger.copy(alpha = 0.35f) else PhasmaInk,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isSel) PhasmaDanger else PhasmaBone.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSel) PhasmaBone else PhasmaBone.copy(alpha = 0.65f),
                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}
