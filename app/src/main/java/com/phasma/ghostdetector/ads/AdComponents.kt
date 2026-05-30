package com.phasma.ghostdetector.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/** AdMob unit IDs, in one place so they're easy to change. */
object AdIds {
    const val BANNER = "ca-app-pub-9148185035281718/2814538627"
    const val INTERSTITIAL = "ca-app-pub-9148185035281718/1131894081"
    // App ID (info only — referenced from AndroidManifest meta-data):
    //   ca-app-pub-9148185035281718~9358245768
}

private const val TAG = "PhasmaAds"

/**
 * AdMob banner wrapped for Compose. Loads on first composition and refreshes
 * automatically per AdMob's server-side rules.
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdIds.BANNER,
    adSize: AdSize = AdSize.BANNER,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() { Log.d(TAG, "banner: loaded") }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "banner: load failed ${error.code} ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

/**
 * Singleton that pre-loads an interstitial and shows it on demand. Rate-limited
 * so it shows at most once per [MIN_INTERVAL_MS] — never spams the user.
 *
 * Lifecycle:
 *   - PhasmaApp.onCreate() → preload(context)
 *   - On a natural break point (e.g. returning from Detector → Home) →
 *     showIfReady(activity) { /* nav.popBackStack() */ }
 *   - Auto-reloads after each show.
 */
object InterstitialManager {

    private const val MIN_INTERVAL_MS = 45_000L

    @Volatile private var ad: InterstitialAd? = null
    @Volatile private var loading = false
    @Volatile private var lastShownMs = 0L

    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            context.applicationContext,
            AdIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    Log.d(TAG, "interstitial: loaded")
                    ad = loaded
                    loading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "interstitial: load failed ${error.code} ${error.message}")
                    ad = null
                    loading = false
                }
            }
        )
    }

    /**
     * Show the interstitial if one is loaded AND enough time has passed since
     * the last one. Otherwise immediately invokes [onComplete]. Either way,
     * [onComplete] is guaranteed to run exactly once, so it's safe to use as
     * a "navigate after ad" hook.
     */
    fun showIfReady(activity: Activity, onComplete: () -> Unit) {
        val now = System.currentTimeMillis()
        val cached = ad
        if (cached == null || now - lastShownMs < MIN_INTERVAL_MS) {
            // Not ready or rate-limited — proceed without showing
            onComplete()
            preload(activity)
            return
        }
        cached.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                lastShownMs = System.currentTimeMillis()
                preload(activity)
                onComplete()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "interstitial: show failed ${error.code} ${error.message}")
                ad = null
                preload(activity)
                onComplete()
            }
            override fun onAdShowedFullScreenContent() {
                // Detach so a second show can't accidentally use the same instance
                ad = null
            }
        }
        cached.show(activity)
    }
}
