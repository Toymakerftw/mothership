package com.toymakerftw.appsage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toymakerftw.appsage.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.toymakerftw.appsage.api.AppsageApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val appsageApi = AppsageApi.create()

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey = _apiKey.asStateFlow()
    
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()
    
    private val _selectedModel = MutableStateFlow("gemini-2.5-flash")  // Default model
    val selectedModel = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro"))
    val availableModels = _availableModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels = _isFetchingModels.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getApiKeyFlow().collect { apiKey ->
                _apiKey.value = apiKey
                if (!apiKey.isNullOrEmpty()) {
                    refreshModels()
                }
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

    fun refreshModels() {
        val currentApiKey = _apiKey.value
        if (currentApiKey.isNullOrEmpty()) return

        viewModelScope.launch {
            _isFetchingModels.value = true
            val models = withContext(Dispatchers.IO) {
                appsageApi.fetchAvailableModels(currentApiKey)
            }
            if (models.isNotEmpty()) {
                _availableModels.value = models
            }
            _isFetchingModels.value = false
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