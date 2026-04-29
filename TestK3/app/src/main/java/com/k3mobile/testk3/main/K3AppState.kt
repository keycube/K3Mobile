package com.k3mobile.testk3.main

import kotlinx.coroutines.channels.Channel

/**
 * Represents a single physical keyboard input event.
 *
 * @property keyCode The Android key code (e.g. [android.view.KeyEvent.KEYCODE_ENTER]).
 * @property unicodeChar The Unicode character produced by the key, or 0 if not printable.
 */
data class K3KeyInput(val keyCode: Int, val unicodeChar: Int = 0)

/**
 * Global singleton managing keyboard event routing and application state flags.
 *
 * Uses a [Channel] instead of SharedFlow to avoid replaying buffered events
 * to new collectors. The channel is recreated on each navigation via
 * [resetKeyChannel], ensuring that only the active screen receives key events.
 *
 * Old collectors (from screens being destroyed) suspend indefinitely on the
 * abandoned channel until their coroutine scope is cancelled by Compose.
 */
object K3AppState {

    /**
     * Current keyboard event channel.
     *
     * Always read via [K3AppState.keyChannel] — never cache the reference,
     * as [resetKeyChannel] replaces it with a new instance.
     */
    @Volatile
    var keyChannel: Channel<K3KeyInput> = Channel(Channel.UNLIMITED)
        private set

    /** `true` when [K3AccessibilityService] is bound and intercepting key events. */
    @Volatile var isServiceConnected: Boolean = false

    /**
     * Creates a fresh key channel, abandoning the previous one.
     *
     * Must be called before every key-triggered navigation to prevent
     * the outgoing screen from consuming events meant for the incoming screen.
     */
    fun resetKeyChannel() {
        keyChannel = Channel(Channel.UNLIMITED)
    }

    /**
     * Sends a key event to the current channel.
     *
     * Uses [Channel.trySend] which never suspends — events are dropped
     * only if the channel is closed (which should not happen in normal use).
     *
     * @param keyCode The Android key code.
     * @param unicodeChar The Unicode character, or 0 for non-printable keys.
     */
    fun emitKey(keyCode: Int, unicodeChar: Int = 0) {
        keyChannel.trySend(K3KeyInput(keyCode, unicodeChar))
    }
}