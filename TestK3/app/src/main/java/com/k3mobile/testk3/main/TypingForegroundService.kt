package com.k3mobile.testk3.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the application process alive during typing sessions.
 *
 * When the screen is off, Android may kill background processes. This service
 * displays a persistent low-priority notification to prevent that, ensuring
 * the [K3AccessibilityService] can continue forwarding keyboard events.
 *
 * Lifecycle:
 * - Started from [MainActivity] when the user enters a typing session ("typing/..." route).
 * - Stopped when the user exits [com.k3mobile.testk3.ui.screens.TypingScreen] (back or finish).
 *
 * The notification text can be updated by re-calling [startService] with a new
 * [EXTRA_STATUS] value.
 */
class TypingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "k3_typing_channel"
        const val NOTIFICATION_ID = 1

        /** Intent extra key for the notification status text. */
        const val EXTRA_STATUS = "extra_status"

        const val STATUS_READY = "Préparez-vous à écrire…"
        const val STATUS_TYPING = "Session de frappe en cours…"
        const val STATUS_DONE = "Session terminée."
    }

    private lateinit var notificationManager: NotificationManager

    @RequiresApi(Build.VERSION_CODES.O)
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

    // -------------------------------------------------------------------------
    // Notification setup
    // -------------------------------------------------------------------------

    /** Creates the notification channel (required on API 26+). */
    @RequiresApi(Build.VERSION_CODES.O)
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

    /**
     * Builds the persistent notification displayed during typing sessions.
     *
     * Tapping the notification reopens [MainActivity] without creating a new instance.
     *
     * @param status Text to display as the notification content.
     * @return The built [Notification].
     */
    private fun buildNotification(status: String): Notification {
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
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
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