package com.example.driver_management_system.domain.repository

import com.example.driver_management_system.domain.model.LoginCredentials
import com.example.driver_management_system.domain.model.LoginResult

interface AuthRepository {
    suspend fun login(credentials: LoginCredentials): LoginResult
}
