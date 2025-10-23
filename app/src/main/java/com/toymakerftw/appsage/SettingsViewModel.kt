package com.toymakerftw.appsage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toymakerftw.appsage.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey = _apiKey.asStateFlow()
    
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()
    
    private val _selectedModel = MutableStateFlow("x-ai/grok-4-fast")  // Default model
    val selectedModel = _selectedModel.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getApiKeyFlow().collect { apiKey ->
                _apiKey.value = apiKey
            }
        }
        viewModelScope.launch {
            settingsRepository.getThemePreferenceFlow().collect { isDarkTheme ->
                _isDarkTheme.value = isDarkTheme
            }
        }
        viewModelScope.launch {
            settingsRepository.getSelectedModelFlow().collect { selectedModel ->
                _selectedModel.value = selectedModel
            }
        }
    }

    fun setApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(apiKey)
            _apiKey.value = apiKey
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            settingsRepository.saveApiKey("")
            _apiKey.value = ""
        }
    }
    
    fun toggleTheme() {
        viewModelScope.launch {
            val currentTheme = _isDarkTheme.value  // Get current theme from the state, not from repo directly
            val newTheme = !currentTheme
            settingsRepository.setThemePreference(newTheme)
            _isDarkTheme.value = newTheme  // Update the state flow
        }
    }
    
    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(isDark)
            _isDarkTheme.value = isDark
        }
    }
    
    fun setSelectedModel(modelId: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedModel(modelId)
            _selectedModel.value = modelId
        }
    }
}