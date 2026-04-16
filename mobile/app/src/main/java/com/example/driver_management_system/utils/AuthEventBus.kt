package com.example.driver_management_system.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bus for authentication events
 * Used to notify UI when token expires (401 Unauthorized)
 */
object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()
    
    suspend fun emit(event: AuthEvent) {
        _events.emit(event)
    }
}

sealed class AuthEvent {
    object TokenExpired : AuthEvent()
    object Unauthorized : AuthEvent()
}
