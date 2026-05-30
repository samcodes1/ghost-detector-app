package com.phasma.ghostdetector

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.phasma.ghostdetector.ads.InterstitialManager
import com.phasma.ghostdetector.audio.SoundManager
import com.phasma.ghostdetector.data.AppSettings

class PhasmaApp : Application() {
    lateinit var sound: SoundManager
        private set
    lateinit var settings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = AppSettings.get(this)
        sound = SoundManager(this).also { sm ->
            // Initial sync from persisted settings
            sm.muted = settings.muted
            sm.sfxVolume = settings.sfxVolume
            sm.ambientVolume = settings.ambientVolume
        }

        // AdMob: initialize once per process, then keep an interstitial pre-loaded
        // so it can show instantly the first time we hit a natural break point.
        MobileAds.initialize(this) {
            InterstitialManager.preload(this)
        }
    }

    override fun onTerminate() {
        sound.release()
        super.onTerminate()
    }

    companion object {
        @Volatile lateinit var instance: PhasmaApp
            private set
    }
}
