package com.toymakerftw.appsage

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.toymakerftw.appsage.data.SettingsRepository
import com.toymakerftw.appsage.data.PwaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(app) {

    private val workManager = WorkManager.getInstance(getApplication())
    private val pwaRepository = PwaRepository(getApplication())

    companion object {
        const val DEFAULT_MODEL_ID = "x-ai/grok-4-fast"
        const val MAX_PROMPT_LENGTH = 2000 // Maximum allowed prompt length
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel
    
    private var generationWorkId: UUID? = null

    init {
        _selectedModel.value = DEFAULT_MODEL_ID
        observeApiKey()
    }

    private fun observeApiKey() {
        viewModelScope.launch {
            settingsRepository.getApiKeyFlow().collect { apiKey ->
                _uiState.value = _uiState.value.copy(apiKey = apiKey)
            }
        }
    }

    fun generatePwa(prompt: String) {
        viewModelScope.launch {
            val apiKey = _uiState.value.apiKey
            if (apiKey.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "API key not set"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                pwaGenerated = false,
                generationStep = 0
            )

            val selectedModelId = _selectedModel.value?.takeIf { it.isNotEmpty() } ?: run {
                Log.w("MainViewModel", "No selected model found, using default")
                DEFAULT_MODEL_ID
            }

            val workRequest = OneTimeWorkRequestBuilder<PwaGenerationWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(PwaGenerationWorker.KEY_PROMPT, prompt)
                        .putString(PwaGenerationWorker.KEY_API_KEY, apiKey)
                        .putString(PwaGenerationWorker.KEY_SELECTED_MODEL, selectedModelId)
                        .build()
                )
                .build()
            
            generationWorkId = workRequest.id
            workManager.enqueue(workRequest)
            
            observeWork(generationWorkId!!)
        }
    }
    
    private fun observeWork(workId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(workId).asFlow().collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val pwaUuid = workInfo.outputData.getString(PwaGenerationWorker.KEY_PWA_UUID)
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                generationStep = null,
                                pwaGenerated = true,
                                pwaUuid = pwaUuid,
                                errorMessage = null
                            )
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(PwaGenerationWorker.KEY_ERROR_MESSAGE)
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                generationStep = null,
                                pwaGenerated = false,
                                pwaUuid = null,
                                errorMessage = error
                            )
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                generationStep = null,
                                errorMessage = "Work was cancelled"
                            )
                        }
                        WorkInfo.State.RUNNING -> {
                            val step = workInfo.progress.getInt(PwaGenerationWorker.KEY_GENERATION_STEP, 0)
                            _uiState.value = _uiState.value.copy(generationStep = step)
                        }
                        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                            // Optionally handle these states if needed
                        }
                    }
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun saveApiKeyAndGeneratePwa(apiKey: String, prompt: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(apiKey)
            _uiState.value = _uiState.value.copy(apiKey = apiKey)
            generatePwa(prompt)
        }
    }
    
    fun deletePwa(uuid: String) {
        viewModelScope.launch {
            val success = pwaRepository.deletePwa(uuid)
            if (success) {
                _uiState.value = _uiState.value.copy(pwaDeleted = true)
            }
        }
    }

    fun clearPwaDeleted() {
        _uiState.value = _uiState.value.copy(pwaDeleted = false)
    }

    suspend fun getPwas(): List<Pair<String, String>> {
        return pwaRepository.getGeneratedPwas()
    }
}

data class MainUiState(
    val isGenerating: Boolean = false,
    val generationStep: Int? = null,
    val pwaGenerated: Boolean = false,
    val pwaUuid: String? = null,
    val errorMessage: String? = null,
    val apiKey: String? = null,
    val pwaDeleted: Boolean = false
)
