package com.example.driver_management_system.data.remote

object ApiConfig {
    // URL is injected via BuildConfig from build.gradle.kts
    // Debug  → http://10.0.2.2:3000/api/v1/ (emulator → host machine)
    // Release → ngrok / production URL
    val BASE_URL: String = BuildConfig.API_BASE_URL

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}

