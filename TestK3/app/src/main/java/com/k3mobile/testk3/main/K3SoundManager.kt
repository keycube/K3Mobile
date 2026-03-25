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
 * K3SoundManager
 *
 * Gère les sons courts (earcons) et le retour haptique de l'application.
 * Le volume des effets est réglable dynamiquement via setVolume().
 */
class K3SoundManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** Volume des effets sonores, 0–100. */
    private var volume: Int = 70

    private var toneGenerator: ToneGenerator? = createToneGenerator(volume)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private fun createToneGenerator(vol: Int): ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, vol.coerceIn(0, 100))
    } catch (_: Exception) {
        null
    }

    /**
     * Change le volume des effets sonores (0–100).
     * Recrée le ToneGenerator car son volume est fixé à la construction.
     */
    fun setVolume(percent: Int) {
        volume = percent.coerceIn(0, 100)
        toneGenerator?.release()
        toneGenerator = createToneGenerator(volume)
    }

    // -------------------------------------------------------------------------
    // Earcons
    // -------------------------------------------------------------------------

    fun playValidation() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    fun playDelete() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
    }

    fun playVictory() {
        scope.launch {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 120)
            delay(160)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_B, 120)
            delay(160)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 250)
        }
    }

    fun playNavigation() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    fun playCountdownTick() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
    }

    fun playCountdownGo() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
    }

    // -------------------------------------------------------------------------
    // Haptique
    // -------------------------------------------------------------------------

    fun vibrateVictory() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 150, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 200, 150, 400), -1)
            }
        } catch (_: SecurityException) { /* permission VIBRATE manquante */ }
    }

    // -------------------------------------------------------------------------
    // Nettoyage
    // -------------------------------------------------------------------------

    fun release() {
        scope.cancel()
        toneGenerator?.release()
        toneGenerator = null
    }
}