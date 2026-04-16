package com.example.driver_management_system.presentation.login

import com.example.driver_management_system.domain.model.User

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val user: User? = null
)
