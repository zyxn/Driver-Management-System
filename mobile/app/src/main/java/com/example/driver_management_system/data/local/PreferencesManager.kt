package com.example.driver_management_system.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val EMAIL_KEY = stringPreferencesKey("email")
        private val FULL_NAME_KEY = stringPreferencesKey("full_name")
        private val ROLE_KEY = stringPreferencesKey("role")
        private val STATUS_KEY = stringPreferencesKey("status")
        private val PHONE_NUMBER_KEY = stringPreferencesKey("phone_number")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        private val SELECTED_RINGTONE_KEY = stringPreferencesKey("selected_ringtone")
    }
    
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
    
    suspend fun saveUserData(
        userId: Int,
        username: String,
        email: String,
        fullName: String,
        role: String,
        status: String,
        phoneNumber: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
            preferences[EMAIL_KEY] = email
            preferences[FULL_NAME_KEY] = fullName
            preferences[ROLE_KEY] = role
            preferences[STATUS_KEY] = status
            phoneNumber?.let { preferences[PHONE_NUMBER_KEY] = it }
        }
    }
    
    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
    
    val userId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }
    
    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }
    
    val email: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[EMAIL_KEY]
    }
    
    val fullName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FULL_NAME_KEY]
    }
    
    val role: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ROLE_KEY]
    }
    
    val status: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[STATUS_KEY]
    }
    
    val phoneNumber: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PHONE_NUMBER_KEY]
    }
    
    // Notification settings
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATION_ENABLED_KEY] ?: true // Default true
    }
    
    val selectedRingtone: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_RINGTONE_KEY] ?: ""
    }
    
    suspend fun saveVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED_KEY] = enabled
        }
        context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("vibration_enabled", enabled)
            .apply()
    }
    
    suspend fun saveSelectedRingtone(ringtone: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_RINGTONE_KEY] = ringtone
        }
        context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
            .edit()
            .putString("selected_ringtone", ringtone)
            .apply()
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY] != null
        }.first()
    }
    
    // Synchronous method for Service usage
    fun getUserId(): Int? {
        val sharedPrefs = context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getInt("user_id", -1)
        return if (userId != -1) userId else null
    }
    
    fun getUserRole(): String? {
        val sharedPrefs = context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
        return sharedPrefs.getString("role", null)
    }

    fun getVibrationEnabled(): Boolean {
        val sharedPrefs = context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("vibration_enabled")) {
            // Fallback to DataStore if SharedPreferences is not yet initialized
            return runBlocking { vibrationEnabled.first() }
        }
        return sharedPrefs.getBoolean("vibration_enabled", true)
    }

    fun getSelectedRingtone(): String {
        val sharedPrefs = context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("selected_ringtone")) {
            // Fallback to DataStore if SharedPreferences is not yet initialized
            return runBlocking { selectedRingtone.first() }
        }
        return sharedPrefs.getString("selected_ringtone", "") ?: ""
    }
    
    suspend fun saveUserIdSync(userId: Int) {
        // Save to both DataStore and SharedPreferences for sync access
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
        context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
            .edit()
            .putInt("user_id", userId)
            .apply()
    }
    
    suspend fun saveRoleSync(role: String) {
        // Save to both DataStore and SharedPreferences for sync access
        context.dataStore.edit { preferences ->
            preferences[ROLE_KEY] = role
        }
        context.getSharedPreferences("user_preferences_sync", Context.MODE_PRIVATE)
            .edit()
            .putString("role", role)
            .apply()
    }
}
