package com.example.driver_management_system.utils

import android.content.Context
import android.location.Location
import android.os.Build
import android.provider.Settings

/**
 * Utility class to detect if mock location is enabled or if a location is from a mock provider
 */
object MockLocationDetector {
    
    /**
     * Check if mock location is enabled in developer options
     * @return true if mock location is enabled, false otherwise
     */
    fun isMockLocationEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android 6.0 and above
                val mockLocationApps = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION
                )
                mockLocationApps != null && mockLocationApps != "0"
            } else {
                // For Android 5.x and below
                @Suppress("DEPRECATION")
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION
                ) != "0"
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if a specific location is from a mock provider
     * @param location The location to check
     * @return true if the location is mocked, false otherwise
     */
    fun isLocationMocked(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            location.isMock
        } else {
            // Android 11 and below
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }
    
    /**
     * Comprehensive check for mock location
     * Checks both developer settings and the location object itself
     * @param context Application context
     * @param location The location to verify
     * @return MockLocationStatus indicating the detection result
     */
    fun checkMockLocation(context: Context, location: Location): MockLocationStatus {
        val settingsEnabled = isMockLocationEnabled(context)
        val locationMocked = isLocationMocked(location)
        
        return when {
            settingsEnabled && locationMocked -> MockLocationStatus.BOTH_DETECTED
            settingsEnabled -> MockLocationStatus.SETTINGS_ENABLED
            locationMocked -> MockLocationStatus.LOCATION_MOCKED
            else -> MockLocationStatus.GENUINE
        }
    }
    
    /**
     * Status enum for mock location detection
     */
    enum class MockLocationStatus {
        GENUINE,              // Location is genuine
        SETTINGS_ENABLED,     // Mock location enabled in settings but this location might be real
        LOCATION_MOCKED,      // This specific location is mocked
        BOTH_DETECTED         // Both settings enabled and location is mocked
    }
}
