package com.k3mobile.testk3.main

import kotlinx.coroutines.channels.Channel

data class K3KeyInput(val keyCode: Int, val unicodeChar: Int = 0)

object K3AppState {

    /**
     * Channel courant pour les événements clavier.
     *
     * On utilise un Channel au lieu d'un SharedFlow pour éviter le rejeu
     * des événements en buffer sur les nouveaux collecteurs.
     *
     * Le channel est recréé à chaque navigation (via resetKeyChannel()).
     * Les anciens collecteurs (écrans en cours de destruction) continuent
     * à lire l'ancien channel qui ne recevra plus rien — ils se suspendent
     * indéfiniment sur receive() puis sont annulés quand leur coroutine
     * scope (LaunchedEffect) est cancelled par Compose.
     *
     * Ainsi, un seul collecteur — celui du nouvel écran — reçoit les touches.
     */
    @Volatile
    var keyChannel: Channel<K3KeyInput> = Channel(Channel.UNLIMITED)
        private set

    @Volatile var isInTypingMode: Boolean = false
    @Volatile var isServiceConnected: Boolean = false

    /**
     * À appeler juste avant toute navigation déclenchée par une touche.
     * Crée un nouveau channel vide. L'ancien channel est abandonné —
     * plus aucune touche ne lui sera envoyée.
     */
    fun resetKeyChannel() {
        keyChannel = Channel(Channel.UNLIMITED)
    }

    fun emitKey(keyCode: Int, unicodeChar: Int = 0) {
        keyChannel.trySend(K3KeyInput(keyCode, unicodeChar))
    }
}
