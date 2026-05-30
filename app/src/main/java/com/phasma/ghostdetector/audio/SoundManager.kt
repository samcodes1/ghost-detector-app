package com.phasma.ghostdetector.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.phasma.ghostdetector.R
import kotlin.random.Random

/**
 * Single hub for every sound the app plays. Use the same instance across the
 * app (one per Activity is fine).
 *
 *  - One-shot stings/screams/whispers play through SoundPool (low latency).
 *  - Long loops (ambient, radio static) play through MediaPlayer (memory friendly).
 *
 * Volume + mute are applied at the manager level so the user's settings work
 * across every screen with no extra plumbing.
 */
class SoundManager(private val context: Context) {

    // --- Public knobs (driven by Settings) ---
    @Volatile var muted: Boolean = false
        set(value) { field = value; if (value) stopAll() }
    @Volatile var sfxVolume: Float = 1f        // 0..1
    @Volatile var ambientVolume: Float = 0.7f  // 0..1

    // --- SoundPool for SFX ---
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = mutableMapOf<Sfx, Int>()

    // --- MediaPlayer instances for loops ---
    private var ambientPlayer: MediaPlayer? = null
    private var radioPlayer: MediaPlayer? = null

    // --- Vibrator ---
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        mgr.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        // Pre-warm every SFX so first play has no jitter
        Sfx.entries.forEach { sfx ->
            loaded[sfx] = pool.load(context, sfx.resId, 1)
        }
    }

    // ─── One-shots ─────────────────────────────────────────────────────────

    fun play(sfx: Sfx, volumeMul: Float = 1f, rate: Float = 1f) {
        if (muted) return
        val id = loaded[sfx] ?: return
        val v = (sfxVolume * volumeMul).coerceIn(0f, 1f)
        pool.play(id, v, v, /*priority=*/ 1, /*loop=*/ 0, rate)
    }

    /** Pick a random SFX from a group (e.g. random scream or random whisper). */
    fun playRandom(group: SfxGroup, volumeMul: Float = 1f) {
        val sfx = group.options.random()
        play(sfx, volumeMul = volumeMul, rate = 0.9f + Random.nextFloat() * 0.2f)
    }

    // ─── Ambient loop ───────────────────────────────────────────────────────

    fun startAmbient(@RawRes resId: Int = R.raw.ambient_horror_drone) {
        if (muted) return
        stopAmbient()
        ambientPlayer = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(ambientVolume, ambientVolume)
            runCatching { start() }
        }
    }

    fun stopAmbient() {
        ambientPlayer?.runCatching { if (isPlaying) stop(); release() }
        ambientPlayer = null
    }

    fun startRadioStatic() {
        if (muted) return
        stopRadioStatic()
        radioPlayer = MediaPlayer.create(context, R.raw.radio_static)?.apply {
            isLooping = true
            setVolume(ambientVolume * 0.55f, ambientVolume * 0.55f)
            runCatching { start() }
        }
    }

    fun stopRadioStatic() {
        radioPlayer?.runCatching { if (isPlaying) stop(); release() }
        radioPlayer = null
    }

    // ─── Vibration helpers ──────────────────────────────────────────────────

    fun vibrateShort() = vibrate(longArrayOf(0, 50))
    fun vibrateAlert() = vibrate(longArrayOf(0, 120, 80, 120))
    fun vibrateJumpScare() = vibrate(
        // attention-shattering: long → bursts → finale
        longArrayOf(0, 600, 60, 80, 60, 80, 60, 200, 60, 600),
        amplitude = 255
    )

    private fun vibrate(pattern: LongArray, amplitude: Int = -1) {
        if (muted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (amplitude > 0) {
                val arr = IntArray(pattern.size) { amplitude }
                VibrationEffect.createWaveform(pattern, arr, -1)
            } else {
                VibrationEffect.createWaveform(pattern, -1)
            }
            runCatching { vibrator.vibrate(effect) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator.vibrate(pattern, -1) }
        }
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────

    fun stopAll() {
        stopAmbient()
        stopRadioStatic()
        pool.autoPause()
    }

    fun release() {
        stopAll()
        runCatching { pool.release() }
    }
}

/** Every sound file in res/raw, typed. */
enum class Sfx(@RawRes val resId: Int) {
    AmbientHorrorDrone(R.raw.ambient_horror_drone),
    AmbientCreepy(R.raw.ambient_creepy),
    StingAppear(R.raw.sting_appear),
    StingLow(R.raw.sting_low),
    StingWhoosh(R.raw.sting_whoosh),
    Whisper1(R.raw.whisper_1),
    Whisper2(R.raw.whisper_2),
    Whisper3(R.raw.whisper_3),
    EmfBeep(R.raw.emf_beep),
    ScreamWoman(R.raw.scream_woman),
    ScreamMan(R.raw.scream_man),
    ScreamLoud(R.raw.scream_loud),
    RadioStatic(R.raw.radio_static),
}

enum class SfxGroup(val options: List<Sfx>) {
    Whispers(listOf(Sfx.Whisper1, Sfx.Whisper2, Sfx.Whisper3)),
    Screams(listOf(Sfx.ScreamWoman, Sfx.ScreamMan, Sfx.ScreamLoud)),
    Stings(listOf(Sfx.StingAppear, Sfx.StingLow, Sfx.StingWhoosh)),
}
