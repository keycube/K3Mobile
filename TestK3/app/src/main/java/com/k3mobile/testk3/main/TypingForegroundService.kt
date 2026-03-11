package com.k3mobile.testk3.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * TypingForegroundService
 *
 * Foreground Service qui maintient le processus de l'application en vie
 * lorsque l'écran est éteint pendant une session de frappe.
 *
 * Cycle de vie :
 *   - Démarré depuis MainActivity quand l'utilisateur lance une partie (route "typing/...")
 *   - Arrêté quand l'utilisateur quitte le TypingScreen (onBack ou onFinished)
 *
 * Pour mettre à jour le texte de la notification, relancez le service via startService()
 * avec un Intent contenant EXTRA_STATUS.
 */
class TypingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID      = "k3_typing_channel"
        const val NOTIFICATION_ID = 1
        /** Clé pour passer le statut affiché dans la notification. */
        const val EXTRA_STATUS    = "extra_status"
        const val STATUS_READY    = "Préparez-vous à écrire…"
        const val STATUS_TYPING   = "Session de frappe en cours…"
        const val STATUS_DONE     = "Session terminée."
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val status = intent?.getStringExtra(EXTRA_STATUS) ?: STATUS_TYPING
        startForeground(NOTIFICATION_ID, buildNotification(status))
        return START_STICKY
    }

    /**
     * Met à jour le texte affiché dans la notification persistante.
     * Peut être appelé depuis n'importe quel thread.
     */
    fun updateStatus(status: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    // -------------------------------------------------------------------------
    // Notification
    // -------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "K3AudioType — Session active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintient la session de frappe active lorsque l'écran est éteint"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(status: String): Notification {
        // Rouvre l'activité principale en tapant sur la notification
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("K3AudioType")
            .setContentText(status)
            // ⚠️  Remplace android.R.drawable.ic_btn_speak_now par ton icône d'app
            //     (ex. R.drawable.ic_notification) pour un rendu soigné en production.
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)       // L'utilisateur ne peut pas swiper pour fermer
            .setSilent(true)        // Pas de son / vibration à chaque mise à jour
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // -------------------------------------------------------------------------

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}