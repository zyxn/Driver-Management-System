package com.example.driver_management_system.presentation.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver_management_system.data.repository.WeatherRepository
import com.example.driver_management_system.domain.model.Weather
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val weather: Weather) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel(
    private val context: Context
) : ViewModel() {
    private val weatherRepository = WeatherRepository()
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()
    
    fun loadWeather() {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            
            try {
                Log.d("WeatherViewModel", "Starting to load weather...")
                
                var location = getCurrentLocation()
                
                // Fallback to Jakarta if GPS fails
                if (location == null) {
                    Log.w("WeatherViewModel", "GPS location failed, using Jakarta as fallback")
                    location = Location("").apply {
                        latitude = -6.2088
                        longitude = 106.8456
                    }
                } else {
                    Log.d("WeatherViewModel", "Got location: ${location.latitude}, ${location.longitude}")
                }
                
                Log.d("WeatherViewModel", "Fetching weather data...")
                val result = weatherRepository.getWeather(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                
                result.fold(
                    onSuccess = { weather ->
                        Log.d("WeatherViewModel", "Weather data loaded successfully")
                        _weatherState.value = WeatherState.Success(weather)
                    },
                    onFailure = { error ->
                        Log.e("WeatherViewModel", "Failed to load weather", error)
                        _weatherState.value = WeatherState.Error(
                            error.message ?: "Gagal memuat data cuaca"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Exception loading weather", e)
                _weatherState.value = WeatherState.Error(
                    e.message ?: "Terjadi kesalahan"
                )
            }
        }
    }
    
    private suspend fun getCurrentLocation(): Location? {
        return try {
            Log.d("WeatherViewModel", "Checking location permission...")
            
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("WeatherViewModel", "Location permission not granted")
                return null
            }
            
            Log.d("WeatherViewModel", "Getting current location...")
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()
            
            Log.d("WeatherViewModel", "Location result: $location")
            location
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error getting location", e)
            null
        }
    }
}
