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
 *
 * Les earcons sont des sons fonctionnels, pas décoratifs :
 *   - Ils donnent un retour instantané sans interrompre le TTS.
 *   - Ils sont essentiels quand l'écran est éteint.
 *
 * Utilise ToneGenerator (sons système) pour éviter d'embarquer des fichiers audio.
 */
class K3SoundManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // ToneGenerator à volume modéré (70%) pour ne pas couvrir le TTS
    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    } catch (_: Exception) {
        null
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // -------------------------------------------------------------------------
    // Earcons
    // -------------------------------------------------------------------------

    /**
     * Son de validation — phrase acceptée, passage à la suivante.
     * Court et positif (≈150ms).
     */
    fun playValidation() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    /**
     * Son de suppression — caractère effacé (DEL).
     * Très court et discret (≈50ms).
     */
    fun playDelete() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
    }

    /**
     * Son de victoire — exercice terminé.
     * Séquence de 3 tons ascendants pour un effet « bravo ».
     */
    fun playVictory() {
        scope.launch {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 120)
            delay(160)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_B, 120)
            delay(160)
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 250)
        }
    }

    /**
     * Son de navigation — changement d'écran.
     * Court et neutre (≈100ms).
     */
    fun playNavigation() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    /**
     * Bip du compte à rebours — remplace ou accompagne le TTS "3, 2, 1".
     * Un bip net et percutant (≈200ms).
     */
    fun playCountdownTick() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
    }

    /**
     * Bip de départ — "Go !" après le compte à rebours.
     * Plus long et distinct du tick (≈350ms).
     */
    fun playCountdownGo() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
    }

    // -------------------------------------------------------------------------
    // Haptique
    // -------------------------------------------------------------------------

    /**
     * Vibration longue — fin d'exercice.
     * Utile si le son est coupé ou si le téléphone est posé sur la table.
     *
     * Nécessite android.permission.VIBRATE dans le Manifest.
     * En cas d'absence de permission, la vibration est ignorée silencieusement.
     */
    fun vibrateVictory() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 150, 400),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 200, 150, 400), -1)
            }
        } catch (_: SecurityException) {
            // Permission VIBRATE manquante — on ignore silencieusement
        }
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