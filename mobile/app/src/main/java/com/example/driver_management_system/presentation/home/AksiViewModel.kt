package com.example.driver_management_system.presentation.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver_management_system.data.local.PreferencesManager
import com.example.driver_management_system.data.repository.ReportRepositoryImpl
import com.example.driver_management_system.domain.usecase.CreateReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AksiUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AksiViewModel(context: Context) : ViewModel() {

    private val preferencesManager = PreferencesManager(context)
    private val reportRepository = ReportRepositoryImpl(context)
    private val createReportUseCase = CreateReportUseCase(reportRepository)

    private val _uiState = MutableStateFlow(AksiUiState())
    val uiState: StateFlow<AksiUiState> = _uiState.asStateFlow()

    fun submitReport(
        placeName: String,
        description: String,
        latitude: Double,
        longitude: Double,
        imageUri: Uri?
    ) {
        if (imageUri == null) {
            _uiState.value = _uiState.value.copy(error = "Foto bukti wajib dilampirkan")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)
            try {
                val userIdStr = preferencesManager.userId.first()
                if (userIdStr == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Sesi telah berakhir, silakan login kembali")
                    return@launch
                }

                val userId = userIdStr.toIntOrNull() ?: 0

                val result = createReportUseCase(
                    userId = userId,
                    placeName = placeName,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    imageUri = imageUri
                )

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal menyimpan aksi"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun resetState() {
        _uiState.value = AksiUiState()
    }
}
