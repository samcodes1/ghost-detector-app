package com.phasma.ghostdetector.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.reflect.KProperty

/**
 * Process-wide settings, persisted via SharedPreferences and observed by
 * Compose through mutableStateOf. Read/write from anywhere; the Compose UI
 * recomposes on change automatically.
 */
class AppSettings private constructor(private val prefs: SharedPreferences) {

    var muted by stored(prefs, "muted", false)
    var vibrationsOn by stored(prefs, "vibrations_on", true)
    /** 0..1 */
    var sfxVolume by stored(prefs, "sfx_volume", 1f)
    var ambientVolume by stored(prefs, "ambient_volume", 0.7f)
    /** 0=mild, 1=medium, 2=insane */
    var scareIntensity by stored(prefs, "scare_intensity", 2)
    var onboardingDone by stored(prefs, "onboarding_done", false)

    companion object {
        @Volatile private var INSTANCE: AppSettings? = null
        fun get(context: Context): AppSettings = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppSettings(
                context.applicationContext.getSharedPreferences("phasma_prefs", Context.MODE_PRIVATE)
            ).also { INSTANCE = it }
        }
    }
}

// ---- tiny stored-state delegate ----

private fun stored(prefs: SharedPreferences, key: String, default: Boolean) =
    PrefStateDelegate(prefs, key, default,
        read = { p, k, d -> p.getBoolean(k, d) },
        write = { p, k, v -> p.edit().putBoolean(k, v).apply() })

private fun stored(prefs: SharedPreferences, key: String, default: Float) =
    PrefStateDelegate(prefs, key, default,
        read = { p, k, d -> p.getFloat(k, d) },
        write = { p, k, v -> p.edit().putFloat(k, v).apply() })

private fun stored(prefs: SharedPreferences, key: String, default: Int) =
    PrefStateDelegate(prefs, key, default,
        read = { p, k, d -> p.getInt(k, d) },
        write = { p, k, v -> p.edit().putInt(k, v).apply() })

private class PrefStateDelegate<T>(
    private val prefs: SharedPreferences,
    private val key: String,
    default: T,
    private val read: (SharedPreferences, String, T) -> T,
    private val write: (SharedPreferences, String, T) -> Unit,
) {
    // Backed by Compose state so reads trigger recomposition
    private var _state by mutableStateOf(read(prefs, key, default))
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T = _state
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        _state = value
        write(prefs, key, value)
    }
}
