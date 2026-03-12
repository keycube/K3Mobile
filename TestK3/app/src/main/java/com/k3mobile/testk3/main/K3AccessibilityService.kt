package com.k3mobile.testk3.main

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class K3AccessibilityService : AccessibilityService() {

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

        val unicode = event.getUnicodeChar(event.metaState)
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