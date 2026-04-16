package com.example.driver_management_system.data.manager

import android.content.Context
import android.util.Log
import com.example.driver_management_system.data.remote.ApiService
import com.example.driver_management_system.data.remote.UpdateFCMTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FCMManager(
    private val context: Context,
    private val apiService: ApiService
) {
    private val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
    
    fun initializeFCM() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM Token: $token")
                
                // Save token locally
                saveTokenLocally(token)
                
                // Send to backend if user is logged in
                sendTokenToBackend(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
            }
        }
    }
    
    fun saveTokenLocally(token: String) {
        prefs.edit().putString("fcm_token", token).apply()
    }
    
    fun getLocalToken(): String? {
        return prefs.getString("fcm_token", null)
    }
    
    fun sendTokenToBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.updateFCMToken(UpdateFCMTokenRequest(token))
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token sent to backend successfully")
                    prefs.edit().putBoolean("token_sent", true).apply()
                } else {
                    Log.e(TAG, "Failed to send FCM token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending FCM token to backend", e)
            }
        }
    }
    
    fun isTokenSent(): Boolean {
        return prefs.getBoolean("token_sent", false)
    }
    
    fun clearTokenSentFlag() {
        prefs.edit().putBoolean("token_sent", false).apply()
    }
    
    companion object {
        private const val TAG = "FCMManager"
    }
}
