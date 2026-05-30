package com.phasma.ghostdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.phasma.ghostdetector.ui.navigation.PhasmaNavHost
import com.phasma.ghostdetector.ui.theme.PhasmaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhasmaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    color = Color.Transparent
                ) {
                    PhasmaNavHost()
                }
            }
        }
    }
}
