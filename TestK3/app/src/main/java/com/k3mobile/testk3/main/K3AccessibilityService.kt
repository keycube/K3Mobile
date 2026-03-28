package com.k3mobile.testk3.main

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class K3AccessibilityService : AccessibilityService() {

    private var pendingDeadKey: Int = 0;

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            eventTypes = 0
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        // Le service est prêt : MainActivity.onKeyDown() doit maintenant se taire
        K3AppState.isServiceConnected = true
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return false

        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false

        var unicode = event.getUnicodeChar(event.metaState)

        if ((unicode and KeyCharacterMap.COMBINING_ACCENT) != 0){
            pendingDeadKey = unicode and KeyCharacterMap.COMBINING_ACCENT_MASK
            return true
        }

        if (pendingDeadKey != 0){
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
        // Service désactivé : le fallback onKeyDown peut reprendre
        K3AppState.isServiceConnected = false
        return super.onUnbind(intent)
    }
}