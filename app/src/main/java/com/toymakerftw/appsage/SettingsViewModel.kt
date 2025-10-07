package com.toymakerftw.appsage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toymakerftw.appsage.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey = _apiKey.asStateFlow()

    init {
        viewModelScope.launch {
            _apiKey.value = settingsRepository.getApiKey()
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
}