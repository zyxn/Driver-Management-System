package com.example.driver_management_system.presentation.settings

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver_management_system.data.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
    private val _selectedRingtone = MutableStateFlow("")
    val selectedRingtone: StateFlow<String> = _selectedRingtone.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.vibrationEnabled.collect { enabled ->
                _vibrationEnabled.value = enabled
            }
        }
        
        viewModelScope.launch {
            preferencesManager.selectedRingtone.collect { ringtone ->
                _selectedRingtone.value = ringtone.ifEmpty {
                    // Get default notification ringtone URI
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
                }
            }
        }
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveVibrationEnabled(enabled)
            _vibrationEnabled.value = enabled
        }
    }
    
    fun setSelectedRingtone(ringtone: String) {
        viewModelScope.launch {
            preferencesManager.saveSelectedRingtone(ringtone)
            _selectedRingtone.value = ringtone
        }
    }
}
