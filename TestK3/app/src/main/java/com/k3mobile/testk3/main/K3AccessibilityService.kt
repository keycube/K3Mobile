package com.k3mobile.testk3.main

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that intercepts physical keyboard events system-wide.
 *
 * This service captures key presses even when the screen is locked or off,
 * which is essential for visually impaired users who rely on physical keyboards.
 * It handles dead key composition (accents like é, è, ê) and forwards
 * processed key events to [K3AppState.keyChannel] for consumption by UI screens.
 *
 * The BACK key is explicitly not consumed, allowing normal system navigation.
 */
class K3AccessibilityService : AccessibilityService() {

    /** Stores the pending dead key (accent) for two-step character composition. */
    private var pendingDeadKey: Int = 0

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            eventTypes = 0
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        K3AppState.isServiceConnected = true
    }

    /**
     * Intercepts key events before they reach the application.
     *
     * Handles dead key composition for accented characters using
     * [KeyCharacterMap.getDeadChar]. For example, pressing '^' then 'e'
     * produces 'ê'.
     *
     * @param event The key event to process.
     * @return `true` if the event was consumed, `false` to let the system handle it.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return false

        // Let the system handle BACK for normal navigation
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false

        var unicode = event.getUnicodeChar(event.metaState)

        // Dead key detected (e.g. accent circumflex, tilde) — store it
        if ((unicode and KeyCharacterMap.COMBINING_ACCENT) != 0) {
            pendingDeadKey = unicode and KeyCharacterMap.COMBINING_ACCENT_MASK
            return true
        }

        // Combine pending dead key with current key if applicable
        if (pendingDeadKey != 0) {
            val combined = KeyCharacterMap.getDeadChar(pendingDeadKey, unicode)
            pendingDeadKey = 0
            unicode = if (combined != 0) combined else unicode
        }

        K3AppState.emitKey(event.keyCode, unicode)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        K3AppState.isServiceConnected = false
        return super.onUnbind(intent)
    }
}