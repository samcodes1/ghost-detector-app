package com.phasma.ghostdetector.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Palette = darkColorScheme(
    primary = PhasmaGlow,
    onPrimary = PhasmaVoid,
    secondary = PhasmaPlasma,
    onSecondary = PhasmaBone,
    tertiary = PhasmaSpectral,
    background = PhasmaVoid,
    onBackground = PhasmaBone,
    surface = PhasmaInk,
    onSurface = PhasmaBone,
    surfaceVariant = PhasmaSmoke,
    error = PhasmaDanger,
    outline = PhasmaMist
)

@Composable
fun PhasmaTheme(
    darkTheme: Boolean = true, // Always dark — it's a ghost app
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PhasmaVoid.toArgb()
            window.navigationBarColor = PhasmaVoid.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = Palette,
        typography = PhasmaTypography,
        content = content
    )
}
