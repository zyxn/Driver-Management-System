package com.example.driver_management_system.presentation.drivers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver_management_system.domain.model.Driver
import com.example.driver_management_system.domain.model.DriverLocation
import com.example.driver_management_system.domain.repository.DriverRepository
import com.example.driver_management_system.domain.usecase.GetAllDriversUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DriverListUiState(
    val drivers: List<Driver> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val addressCache: Map<String, String> = emptyMap()
)

class DriverListViewModel(
    private val getAllDriversUseCase: GetAllDriversUseCase,
    private val driverRepository: DriverRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DriverListUiState())
    val uiState: StateFlow<DriverListUiState> = _uiState.asStateFlow()
    
    init {
        loadDrivers()
    }
    
    fun loadDrivers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            getAllDriversUseCase().fold(
                onSuccess = { drivers ->
                    _uiState.value = _uiState.value.copy(
                        drivers = drivers,
                        isLoading = false
                    )
                    
                    // Load addresses for drivers with locations
                    drivers.forEach { driver ->
                        driver.lastLocation?.let { location ->
                            loadAddressForLocation(driver.id, location)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Gagal memuat data driver"
                    )
                }
            )
        }
    }
    
    private fun loadAddressForLocation(driverId: Int, location: DriverLocation) {
        viewModelScope.launch {
            val cacheKey = "${location.latitude},${location.longitude}"
            
            // Check if already cached
            if (_uiState.value.addressCache.containsKey(cacheKey)) {
                updateDriverAddress(driverId, _uiState.value.addressCache[cacheKey]!!)
                return@launch
            }
            
            driverRepository.getAddressFromCoordinates(
                location.latitude,
                location.longitude
            ).fold(
                onSuccess = { address ->
                    // Update cache
                    _uiState.value = _uiState.value.copy(
                        addressCache = _uiState.value.addressCache + (cacheKey to address)
                    )
                    
                    // Update driver with address
                    updateDriverAddress(driverId, address)
                },
                onFailure = {
                    // Silently fail, keep default address
                }
            )
        }
    }
    
    private fun updateDriverAddress(driverId: Int, address: String) {
        _uiState.value = _uiState.value.copy(
            drivers = _uiState.value.drivers.map { driver ->
                if (driver.id == driverId && driver.lastLocation != null) {
                    driver.copy(
                        lastLocation = driver.lastLocation.copy(address = address)
                    )
                } else {
                    driver
                }
            }
        )
    }
    
    fun refresh() {
        loadDrivers()
    }
}
