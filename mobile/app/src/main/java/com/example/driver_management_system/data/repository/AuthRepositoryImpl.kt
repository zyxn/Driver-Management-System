package com.example.driver_management_system.data.repository

import android.content.Context
import com.example.driver_management_system.data.local.PreferencesManager
import com.example.driver_management_system.data.remote.RetrofitClient
import com.example.driver_management_system.data.remote.dto.ErrorResponse
import com.example.driver_management_system.data.remote.dto.LoginRequest
import com.example.driver_management_system.domain.model.LoginCredentials
import com.example.driver_management_system.domain.model.LoginResult
import com.example.driver_management_system.domain.model.User
import com.example.driver_management_system.domain.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(private val context: Context) : AuthRepository {
    private val apiService = RetrofitClient.apiService
    private val preferencesManager = PreferencesManager(context)
    private val gson = Gson()
    
    override suspend fun login(credentials: LoginCredentials): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(
                    email = credentials.email,
                    password = credentials.password
                )
                
                val response = apiService.login(request)
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.data != null) {
                        val userDto = loginResponse.data.user
                        
                        // Extract token from Set-Cookie header
                        val cookies = response.headers()["Set-Cookie"]
                        val token = cookies?.let { extractTokenFromCookie(it) } ?: ""
                        
                        // Save token and user data
                        preferencesManager.saveToken(token)
                        preferencesManager.saveUserData(
                            userId = userDto.id,
                            username = userDto.username,
                            email = userDto.email,
                            fullName = userDto.fullName,
                            role = userDto.role,
                            status = userDto.status
                        )
                        // Save userId and role synchronously for Service access
                        preferencesManager.saveUserIdSync(userDto.id)
                        preferencesManager.saveRoleSync(userDto.role)
                        
                        val user = User(
                            id = userDto.id,
                            username = userDto.username,
                            email = userDto.email,
                            fullName = userDto.fullName,
                            phone = userDto.phone,
                            licenseNo = userDto.licenseNo,
                            role = userDto.role,
                            status = userDto.status
                        )
                        
                        LoginResult.Success(token, user)
                    } else {
                        LoginResult.Error("Login gagal: Response tidak valid")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.error
                    } catch (e: Exception) {
                        "Login gagal: ${response.message()}"
                    }
                    LoginResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                LoginResult.Error("Koneksi gagal: ${e.message ?: "Tidak dapat terhubung ke server"}")
            }
        }
    }
    
    private fun extractTokenFromCookie(cookieHeader: String): String {
        // Extract token value from Set-Cookie header
        // Format: token=<value>; Path=/; HttpOnly; ...
        val tokenPrefix = "token="
        val startIndex = cookieHeader.indexOf(tokenPrefix)
        if (startIndex == -1) return ""
        
        val tokenStart = startIndex + tokenPrefix.length
        val tokenEnd = cookieHeader.indexOf(";", tokenStart)
        
        return if (tokenEnd != -1) {
            cookieHeader.substring(tokenStart, tokenEnd)
        } else {
            cookieHeader.substring(tokenStart)
        }
    }
}
