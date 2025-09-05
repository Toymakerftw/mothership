package com.example.mothership

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mothership.api.MothershipApi
import com.example.mothership.data.SettingsRepository
import com.example.mothership.demo.DemoKeyManager
import com.example.mothership.work.PwaWorkManager
import com.example.mothership.work.PwaGenerationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.System

class MainViewModel(
    private val mothershipApi: MothershipApi,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    private val demoKeyManager = DemoKeyManager(context)
    private val pwaWorkManager = PwaWorkManager.getInstance(context)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _pwas = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pwas = _pwas.asStateFlow()

    // Prompt state for main screen
    private val _prompt = MutableStateFlow("")
    val prompt = _prompt.asStateFlow()

    fun setPrompt(value: String) {
        _prompt.value = value
    }

    fun clearPrompt() {
        _prompt.value = ""
    }

    fun generatePwa(prompt: String) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                // Get the API key - first try user key, then demo key
                val apiKey = getApiKeyForRequest()
                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = MainUiState.Error("API key not set. Please go to Settings to add your OpenRouter API key.")
                    return@launch
                }

                if (prompt.isBlank()) {
                    _uiState.value = MainUiState.Error("Please enter a description for your app.")
                    return@launch
                }

                // Enqueue the work using WorkManager
                pwaWorkManager.enqueuePwaGeneration(prompt, prompt)

                // Trigger garbage collection to reduce memory pressure
                System.gc()
                
                _uiState.value = MainUiState.Success
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception in generatePwa", e)
                _uiState.value = MainUiState.Error("Failed to generate app: ${e.message ?: "Unknown error"}. Please try again.")
            }
        }
    }
    
    /**
     * Gets the appropriate API key for the request:
     * 1. User-provided key (if exists)
     * 2. Demo key (if available and under limit)
     */
    private suspend fun getApiKeyForRequest(): String? {
        Log.d("MainViewModel", "Getting API key for request")
        
        // First, try the user-provided API key
        val userApiKey = settingsRepository.getApiKey()
        if (!userApiKey.isNullOrEmpty()) {
            Log.d("MainViewModel", "Using user-provided API key")
            return userApiKey
        }
        
        Log.d("MainViewModel", "No user API key found, checking demo key")
        
        // If no user key, check if we can use demo key
        if (!demoKeyManager.canUseDemoKey()) {
            Log.d("MainViewModel", "Demo limit reached")
            _uiState.value = MainUiState.Error("Demo limit reached (5 calls/day). Please add your own OpenRouter API key in Settings to continue using Mothership.")
            return null
        }
        
        Log.d("MainViewModel", "Demo limit not reached, fetching demo key")
        
        // Try to get existing demo key or fetch a new one
        var demoApiKey = demoKeyManager.getDemoApiKey()
        if (demoApiKey.isNullOrEmpty()) {
            Log.d("MainViewModel", "No existing demo key found, registering device and fetching new one")
            // Clear existing device ID to force fresh registration
            demoKeyManager.clearDeviceId()
            // Register device
            demoKeyManager.registerDevice()
            // Fetch a new demo API key
            demoApiKey = demoKeyManager.fetchDemoApiKey()
            if (demoApiKey.isNullOrEmpty()) {
                Log.d("MainViewModel", "Failed to fetch demo API key")
                _uiState.value = MainUiState.Error("Failed to fetch demo API key. Please add your own OpenRouter API key in Settings.")
                return null
            }
        }
        
        Log.d("MainViewModel", "Using demo API key")
        return demoApiKey
    }

    fun deletePwa(uuid: String) {
        viewModelScope.launch {
            val pwaDir = java.io.File(context.getExternalFilesDir(null), uuid)
            if (pwaDir.exists()) {
                // First uninstall from desktop
                val installer = PwaInstaller(context)
                installer.uninstall(uuid)
                
                // Then delete the files
                pwaDir.deleteRecursively()
                getPwas()
            }
        }
    }

    fun getPwas() {
        viewModelScope.launch {
            try {
                val pwaDir = context.getExternalFilesDir(null)
                if (pwaDir != null && pwaDir.exists()) {
                    _pwas.value = pwaDir.listFiles()?.mapNotNull { 
                        if (it.isDirectory) {
                            val appInfoFile = java.io.File(it, "app_info.json")
                            if (appInfoFile.exists()) {
                                try {
                                    val appInfo = appInfoFile.readText()
                                    val jsonObject = org.json.JSONObject(appInfo)
                                    val pwaName = jsonObject.optString("name", "Untitled App")
                                    it.name to pwaName
                                } catch (e: Exception) {
                                    // Handle corrupted app_info.json files
                                    null
                                }
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } ?: emptyList()
                }
            } catch (e: Exception) {
                _pwas.value = emptyList()
            }
        }
    }
}

sealed class MainUiState {
    object Idle : MainUiState()
    object Loading : MainUiState()
    object Success : MainUiState()
    data class Error(val message: String) : MainUiState()
}