package com.toymakerftw.appsage.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.toymakerftw.appsage.data.SettingsRepository

class ThemeState(private val settingsRepository: SettingsRepository) {
    var isDarkTheme by mutableStateOf(false)
        private set

    suspend fun initializeTheme() {
        isDarkTheme = settingsRepository.getThemePreference()
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        updateThemePreference()
    }

    fun setTheme(dark: Boolean) {
        isDarkTheme = dark
        updateThemePreference()
    }

    private fun updateThemePreference() {
        // Launch a coroutine to update the settings in the background
        // Since we can't directly launch coroutines here, we'll update settings differently
        // Actually, let's have the calling component handle the save operation
    }
}

val LocalThemeState = staticCompositionLocalOf<ThemeState> {
    error("No ThemeState provided")
}