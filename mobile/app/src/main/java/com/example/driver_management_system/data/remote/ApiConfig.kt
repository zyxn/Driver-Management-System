package com.example.driver_management_system.data.remote

object ApiConfig {
    // Gunakan ngrok URL untuk testing
    const val BASE_URL = "https://ember-parted-unquizzically.ngrok-free.dev/api/v1/"
    
    // Alternatif untuk localhost (perlu network security config):
    // const val BASE_URL = "http://10.0.2.2:8080/api/v1/" // Android emulator
    // const val BASE_URL = "http://192.168.1.XXX:8080/api/v1/" // Device fisik
    
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
