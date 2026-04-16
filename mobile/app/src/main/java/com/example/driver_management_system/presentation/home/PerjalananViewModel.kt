package com.example.driver_management_system.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver_management_system.data.local.PreferencesManager
import com.example.driver_management_system.data.remote.RetrofitClient
import com.example.driver_management_system.data.repository.ReportRepositoryImpl
import com.example.driver_management_system.domain.model.TripHistory
import com.example.driver_management_system.domain.usecase.GetUserTripsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

data class PerjalananUiState(
    val isLoading: Boolean = false,
    val trips: List<TripHistory> = emptyList(),
    val locationHistory: List<GeoPoint> = emptyList(),
    val selectedDate: String = getCurrentDate(),
    val error: String? = null
)

class PerjalananViewModel(context: Context) : ViewModel() {
    
    private val preferencesManager = PreferencesManager(context)
    
    // Clean Architecture: Repository -> UseCase -> ViewModel
    private val reportRepository = ReportRepositoryImpl(context)
    private val getUserTripsUseCase = GetUserTripsUseCase(reportRepository)
    
    private val _uiState = MutableStateFlow(PerjalananUiState())
    val uiState: StateFlow<PerjalananUiState> = _uiState.asStateFlow()
    
    fun loadTripsForDate(dateString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedDate = dateString,
                error = null
            )
            
            try {
                val userId = preferencesManager.userId.first()
                android.util.Log.d("PerjalananViewModel", "Loading trips for userId: $userId, date: $dateString")
                
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not logged in"
                    )
                    return@launch
                }
                
                // Use case handles all business logic
                val result = getUserTripsUseCase(userId, dateString)
                
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    android.util.Log.d("PerjalananViewModel", "Trips loaded: ${data?.trips?.size}, Location history: ${data?.locationHistory?.size}")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        trips = data?.trips ?: emptyList(),
                        locationHistory = data?.locationHistory ?: emptyList(),
                        error = null
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to load trips"
                    android.util.Log.e("PerjalananViewModel", "Error loading trips: $errorMsg")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PerjalananViewModel", "Exception loading trips", e)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun setSelectedDate(dateString: String) {
        loadTripsForDate(dateString)
    }
}

private fun getCurrentDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return dateFormat.format(Date())
}
