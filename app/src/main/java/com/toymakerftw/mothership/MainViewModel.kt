package com.toymakerftw.mothership

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toymakerftw.mothership.api.MothershipApi
import com.toymakerftw.mothership.data.SettingsRepository
import com.toymakerftw.mothership.demo.DemoKeyManager
import com.toymakerftw.mothership.work.PwaWorkManager
import com.toymakerftw.mothership.work.PwaGenerationWorker
import com.toymakerftw.mothership.work.PwaReworkWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.System
import java.util.UUID
import androidx.work.WorkInfo

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

    // Track the current work ID for PWA generation
    private var currentWorkId: UUID? = null

    fun setPrompt(value: String) {
        _prompt.value = value
    }

    fun clearPrompt() {
        _prompt.value = ""
    }

    fun retryGeneration() {
        val currentPrompt = _prompt.value
        if (currentPrompt.isNotBlank()) {
            generatePwa(currentPrompt)
        }
    }

    fun clearError() {
        if (_uiState.value is MainUiState.Error) {
            _uiState.value = MainUiState.Idle
        }
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
                val workId = pwaWorkManager.enqueuePwaGeneration(prompt, prompt)
                currentWorkId = workId

                // Trigger garbage collection to reduce memory pressure
                System.gc()
                
                // Observe the work state to update UI accordingly
                observeWorkState(workId)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception in generatePwa", e)
                val errorState = when {
                    e.message?.contains("API key", ignoreCase = true) == true -> 
                        MainUiState.Error(
                            "API key issue: ${e.message}. Please check your settings.",
                            ErrorType.API_KEY,
                            canRetry = false
                        )
                    e.message?.contains("network", ignoreCase = true) == true || 
                    e.message?.contains("connection", ignoreCase = true) == true ->
                        MainUiState.Error(
                            "Network error: ${e.message}. Please check your internet connection and try again.",
                            ErrorType.NETWORK,
                            canRetry = true
                        )
                    e.message?.contains("demo", ignoreCase = true) == true ->
                        MainUiState.Error(
                            "Demo limit reached: ${e.message}. Please add your own API key in Settings.",
                            ErrorType.DEMO_LIMIT,
                            canRetry = false
                        )
                    else ->
                        MainUiState.Error(
                            "Failed to generate app: ${e.message ?: "Unknown error"}. Please try again.",
                            ErrorType.UNKNOWN,
                            canRetry = true
                        )
                }
                _uiState.value = errorState
            }
        }
    }
    
    fun reworkPwa(uuid: String, pwaName: String, prompt: String) {
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
                    _uiState.value = MainUiState.Error("Please enter a description for reworking your app.")
                    return@launch
                }

                // Enqueue the rework using WorkManager
                val workId = pwaWorkManager.enqueuePwaRework(prompt, uuid, pwaName)
                currentWorkId = workId

                // Trigger garbage collection to reduce memory pressure
                System.gc()
                
                // Observe the work state to update UI accordingly
                observeReworkState(workId)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception in reworkPwa", e)
                val errorState = when {
                    e.message?.contains("API key", ignoreCase = true) == true -> 
                        MainUiState.Error(
                            "API key issue: ${e.message}. Please check your settings.",
                            ErrorType.API_KEY,
                            canRetry = false
                        )
                    e.message?.contains("network", ignoreCase = true) == true || 
                    e.message?.contains("connection", ignoreCase = true) == true ->
                        MainUiState.Error(
                            "Network error: ${e.message}. Please check your internet connection and try again.",
                            ErrorType.NETWORK,
                            canRetry = true
                        )
                    e.message?.contains("demo", ignoreCase = true) == true ->
                        MainUiState.Error(
                            "Demo limit reached: ${e.message}. Please add your own API key in Settings.",
                            ErrorType.DEMO_LIMIT,
                            canRetry = false
                        )
                    else ->
                        MainUiState.Error(
                            "Failed to rework app: ${e.message ?: "Unknown error"}. Please try again.",
                            ErrorType.UNKNOWN,
                            canRetry = true
                        )
                }
                _uiState.value = errorState
            }
        }
    }
    
    private fun observeWorkState(workId: UUID) {
        viewModelScope.launch {
            pwaWorkManager.getWorkState(workId).collect { state ->
                when (state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.value = MainUiState.Success
                        // Clear the prompt after successful generation
                        _prompt.value = ""
                        // Refresh the PWA list
                        getPwas()
                        currentWorkId = null
                    }
                    WorkInfo.State.FAILED -> {
                        // Get detailed error information from WorkManager
                        val workInfo = pwaWorkManager.getWorkInfo(workId).first()
                        val errorMessage = workInfo?.outputData?.getString(PwaGenerationWorker.KEY_ERROR_MESSAGE)
                            ?: "Failed to generate PWA. Please check your network connection and try again."
                        
                        val errorState = when {
                            errorMessage.contains("API key", ignoreCase = true) ->
                                MainUiState.Error(errorMessage, ErrorType.API_KEY, canRetry = false)
                            errorMessage.contains("network", ignoreCase = true) || 
                            errorMessage.contains("connection", ignoreCase = true) ->
                                MainUiState.Error(errorMessage, ErrorType.NETWORK, canRetry = true)
                            errorMessage.contains("demo", ignoreCase = true) ->
                                MainUiState.Error(errorMessage, ErrorType.DEMO_LIMIT, canRetry = false)
                            else ->
                                MainUiState.Error(errorMessage, ErrorType.WORK_MANAGER, canRetry = true)
                        }
                        _uiState.value = errorState
                        currentWorkId = null
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.value = MainUiState.Error(
                            "PWA generation was cancelled. This might be due to network issues or app being backgrounded.",
                            ErrorType.NETWORK,
                            canRetry = true
                        )
                        currentWorkId = null
                    }
                    else -> {
                        // Keep in loading state for RUNNING, ENQUEUED, etc.
                    }
                }
            }
        }
    }
    
    private fun observeReworkState(workId: UUID) {
        viewModelScope.launch {
            pwaWorkManager.getWorkState(workId).collect { state ->
                when (state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.value = MainUiState.Success
                        // Clear the prompt after successful rework
                        _prompt.value = ""
                        // Refresh the PWA list
                        getPwas()
                        currentWorkId = null
                    }
                    WorkInfo.State.FAILED -> {
                        // Get detailed error information from WorkManager
                        val workInfo = pwaWorkManager.getWorkInfo(workId).first()
                        val errorMessage = workInfo?.outputData?.getString(PwaReworkWorker.KEY_ERROR_MESSAGE)
                            ?: "Failed to rework PWA. Please check your network connection and try again."
                        
                        val errorState = when {
                            errorMessage.contains("API key", ignoreCase = true) ->
                                MainUiState.Error(errorMessage, ErrorType.API_KEY, canRetry = false)
                            errorMessage.contains("network", ignoreCase = true) || 
                            errorMessage.contains("connection", ignoreCase = true) ->
                                MainUiState.Error(errorMessage, ErrorType.NETWORK, canRetry = true)
                            errorMessage.contains("demo", ignoreCase = true) ->
                                MainUiState.Error(errorMessage, ErrorType.DEMO_LIMIT, canRetry = false)
                            else ->
                                MainUiState.Error(errorMessage, ErrorType.WORK_MANAGER, canRetry = true)
                        }
                        _uiState.value = errorState
                        currentWorkId = null
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.value = MainUiState.Error(
                            "PWA rework was cancelled. This might be due to network issues or app being backgrounded.",
                            ErrorType.NETWORK,
                            canRetry = true
                        )
                        currentWorkId = null
                    }
                    else -> {
                        // Keep in loading state for RUNNING, ENQUEUED, etc.
                    }
                }
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
    object Success : MainUiState() {
        val message: String = "PWA generated successfully!"
    }
    data class Error(
        val message: String,
        val errorType: ErrorType = ErrorType.UNKNOWN,
        val canRetry: Boolean = true
    ) : MainUiState()
}

enum class ErrorType {
    NETWORK,
    API_KEY,
    DEMO_LIMIT,
    WORK_MANAGER,
    UNKNOWN
}