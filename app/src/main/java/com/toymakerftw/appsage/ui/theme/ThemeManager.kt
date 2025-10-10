package com.toymakerftw.appsage.ui.theme

import android.app.Application
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun AppsageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            tertiary = Color(0xFF3700B3),
            background = Color(0xFF000000),
            surface = Color(0xFF121212),
            onPrimary = Color(0xFF000000),
            onSecondary = Color(0xFF000000),
            onTertiary = Color(0xFF000000),
            onBackground = Color(0xFFFFFFFF),
            onSurface = Color(0xFFFFFFFF),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6),
            tertiary = Color(0xFF3700B3),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onTertiary = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000),
            onSurface = Color(0xFF000000),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        ),
        content = content
    )
}