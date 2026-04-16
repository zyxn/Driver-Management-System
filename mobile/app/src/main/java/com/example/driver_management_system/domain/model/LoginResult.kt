package com.example.driver_management_system.domain.model

sealed class LoginResult {
    data class Success(val token: String, val user: User) : LoginResult()
    data class Error(val message: String) : LoginResult()
}
