package com.k3mobile.testk3.main

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object K3AppState {

    private val _keyEvent = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val keyEvent: SharedFlow<Int> = _keyEvent.asSharedFlow()

    @Volatile
    var isInTypingMode: Boolean = false

    fun emitKey(keyCode: Int) {
        if (!isInTypingMode) {
            _keyEvent.tryEmit(keyCode)
        }
    }
}