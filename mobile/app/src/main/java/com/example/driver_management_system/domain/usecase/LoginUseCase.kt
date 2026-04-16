package com.example.driver_management_system.domain.usecase

import android.util.Patterns
import com.example.driver_management_system.domain.model.LoginCredentials
import com.example.driver_management_system.domain.model.LoginResult
import com.example.driver_management_system.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): LoginResult {
        // Validasi input
        if (email.isBlank()) {
            return LoginResult.Error("Email tidak boleh kosong")
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return LoginResult.Error("Format email tidak valid")
        }
        
        if (password.isBlank()) {
            return LoginResult.Error("Password tidak boleh kosong")
        }
        
        if (password.length < 6) {
            return LoginResult.Error("Password minimal 6 karakter")
        }
        
        return repository.login(LoginCredentials(email, password))
    }
}
