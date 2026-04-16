package com.example.driver_management_system

import android.app.Application
import com.example.driver_management_system.data.remote.RetrofitClient

class DriverManagementApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RetrofitClient with application context
        RetrofitClient.init(applicationContext)
    }
}
