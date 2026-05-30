package com.phasma.ghostdetector.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phasma.ghostdetector.ads.InterstitialManager
import com.phasma.ghostdetector.data.AppSettings
import com.phasma.ghostdetector.ui.detector.DetectorScreen
import com.phasma.ghostdetector.ui.home.HomeScreen
import com.phasma.ghostdetector.ui.onboarding.OnboardingScreen
import com.phasma.ghostdetector.ui.prank.PrankScreen
import com.phasma.ghostdetector.ui.settings.SettingsScreen
import com.phasma.ghostdetector.ui.spiritbox.SpiritBoxScreen
import com.phasma.ghostdetector.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DETECTOR = "detector"
    const val PRANK = "prank"
    const val SPIRIT_BOX = "spirit_box"
    const val SETTINGS = "settings"
}

@Composable
fun PhasmaNavHost() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val activity = context as? Activity

    // Helper that pops back to Home, but tries to show an interstitial first.
    // The interstitial is rate-limited (45 s minimum gap) and only fires when
    // pre-loaded, so it never delays the user noticeably.
    fun exitWithInterstitial() {
        val a = activity
        if (a != null) {
            InterstitialManager.showIfReady(a) { nav.popBackStack() }
        } else {
            nav.popBackStack()
        }
    }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                val next = if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING
                nav.navigate(next) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartHunt = { nav.navigate(Routes.DETECTOR) },
                onStartPrank = { nav.navigate(Routes.PRANK) },
                onStartSpiritBox = { nav.navigate(Routes.SPIRIT_BOX) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.DETECTOR) {
            DetectorScreen(onExit = { exitWithInterstitial() })
        }
        composable(Routes.PRANK) {
            // Interstitial only after the scare has played through — feels natural,
            // never interrupts the gag.
            PrankScreen(onExit = { exitWithInterstitial() })
        }
        composable(Routes.SPIRIT_BOX) {
            SpiritBoxScreen(onExit = { exitWithInterstitial() })
        }
        composable(Routes.SETTINGS) {
            // Settings is a quick in-and-out — no interstitial here, would be spammy.
            SettingsScreen(onExit = { nav.popBackStack() })
        }
    }
}
