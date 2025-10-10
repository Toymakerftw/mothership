package com.toymakerftw.appsage.ui.theme

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.toymakerftw.appsage.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    app: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(app) {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        loadThemePreference()
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            settingsRepository.getThemePreferenceFlow().collect { isDarkTheme ->
                _isDarkTheme.value = isDarkTheme
            }
        }
    }

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        viewModelScope.launch {
            settingsRepository.setThemePreference(newValue)
        }
    }

    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        viewModelScope.launch {
            settingsRepository.setThemePreference(isDark)
        }
    }
}