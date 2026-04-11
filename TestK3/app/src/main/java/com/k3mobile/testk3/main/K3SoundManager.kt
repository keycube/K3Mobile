package com.k3mobile.testk3.main

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*

/**
 * Manages short audio feedback (earcons) and haptic feedback for the application.
 *
 * Uses [ToneGenerator] for sound effects and [Vibrator] for haptic patterns.
 * The effects volume is adjustable at runtime via [setVolume]; changing it
 * recreates the [ToneGenerator] since its volume is fixed at construction.
 *
 * @param context Application context, used to obtain system services.
 */
class K3SoundManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** Current effects volume, 0–100. */
    private var volume: Int = 70

    /** When `false`, all vibration calls are silently ignored. */
    var vibrationEnabled: Boolean = true

    private var toneGenerator: ToneGenerator? = createToneGenerator(volume)

    /** Coroutine scope for multi-tone sequences (e.g. victory jingle). */
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Safely creates a [ToneGenerator] at the given volume.
     *
     * @param vol Volume percentage, clamped to 0–100.
     * @return A new [ToneGenerator] or `null` if creation fails.
     */
    private fun createToneGenerator(vol: Int): ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, vol.coerceIn(0, 100))
    } catch (_: Exception) {
        null
    }

    /**
     * Updates the effects volume and recreates the tone generator.
     *
     * @param percent New volume percentage (0–100).
     */
    fun setVolume(percent: Int) {
        volume = percent.coerceIn(0, 100)
        toneGenerator?.release()
        toneGenerator = createToneGenerator(volume)
    }

    // -------------------------------------------------------------------------
    // Earcons
    // -------------------------------------------------------------------------

    /** Plays a short confirmation tone when a sentence is validated. */
    fun playValidation() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    /** Plays a brief pip when a character is deleted. */
    fun playDelete() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
    }

    /** Plays a three-note ascending jingle on exercise completion. */
    fun playVictory() {
        scope.launch {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 120)
            delay(160)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_B, 120)
            delay(160)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 250)
        }
    }

    /** Plays a beep when navigating between screens. */
    fun playNavigation() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    /** Plays the countdown tick sound (3, 2, 1). */
    fun playCountdownTick() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
    }

    /** Plays the "go" sound at countdown end. */
    fun playCountdownGo() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
    }

    // -------------------------------------------------------------------------
    // Haptic feedback
    // -------------------------------------------------------------------------

    /**
     * Triggers a victory vibration pattern (short-pause-long).
     *
     * Respects the [vibrationEnabled] flag and catches [SecurityException]
     * for devices where the VIBRATE permission may be revoked at runtime.
     */
    fun vibrateVictory() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 150, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 200, 150, 400), -1)
            }
        } catch (_: SecurityException) { }
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    /** Releases the tone generator and cancels pending coroutines. */
    fun release() {
        scope.cancel()
        toneGenerator?.release()
        toneGenerator = null
    }
}