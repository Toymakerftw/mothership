package com.example.mothership

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mothership.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    init {
        loadApiKey()
    }

    private fun loadApiKey() {
        viewModelScope.launch {
            _apiKey.value = settingsRepository.getOpenRouterApiKey() ?: ""
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.saveOpenRouterApiKey(apiKey)
            _apiKey.value = apiKey
        }
    }
}