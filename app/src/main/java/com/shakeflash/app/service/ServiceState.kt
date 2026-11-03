package com.shakeflash.app.service

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide, observable state of the foreground service. The UI reads this to show
 * "Active / Off" without having to bind to the service.
 */
object ServiceState {

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _detecting = MutableStateFlow(false)
    val detecting: StateFlow<Boolean> = _detecting.asStateFlow()

    internal fun setRunning(value: Boolean) {
        _running.value = value
        if (!value) _detecting.value = false
    }

    internal fun setDetecting(value: Boolean) {
        _detecting.value = value
    }
}

/** One-shot messages from the service to the UI (torch errors, blocked starts, …). */
object ServiceEvents {

    sealed interface Event {
        data class Error(val message: String) : Event
        data object PermissionRequired : Event
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun emit(event: Event) {
        _events.tryEmit(event)
    }
}
