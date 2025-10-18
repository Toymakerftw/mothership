package com.toymakerftw.appsage

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.toymakerftw.appsage.data.SettingsRepository
import com.toymakerftw.appsage.versioncontrol.VersionControl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReworkViewModel @Inject constructor(
    app: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(app) {

    private val versionControl = VersionControl(getApplication())
    private val workManager = WorkManager.getInstance(getApplication())

    private val _uiState = MutableStateFlow(ReworkUiState())
    val uiState: StateFlow<ReworkUiState> = _uiState

    private var reworkWorkId: UUID? = null

    fun reworkPwa(uuid: String, reworkPrompt: String) {
        viewModelScope.launch {
            val apiKey = settingsRepository.getApiKey()
            if (apiKey.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(isReworking = false, errorMessage = "API key not set")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isReworking = true, errorMessage = null, generationStep = 0)

            val workRequest = OneTimeWorkRequestBuilder<PwaReworkWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(PwaReworkWorker.KEY_UUID, uuid)
                        .putString(PwaReworkWorker.KEY_REWORK_PROMPT, reworkPrompt)
                        .putString(PwaReworkWorker.KEY_API_KEY, apiKey)
                        .build()
                )
                .build()

            reworkWorkId = workRequest.id
            workManager.enqueue(workRequest)

            observeWork(reworkWorkId!!)
        }
    }

    private fun observeWork(workId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(workId).asFlow().collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _uiState.value = _uiState.value.copy(
                                isReworking = false,
                                pwaReworked = true,
                                generationStep = null
                            )
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(PwaReworkWorker.KEY_ERROR_MESSAGE)
                            _uiState.value = _uiState.value.copy(
                                isReworking = false,
                                errorMessage = error,
                                generationStep = null
                            )
                        }
                        WorkInfo.State.RUNNING -> {
                            val step = workInfo.progress.getInt(PwaReworkWorker.KEY_GENERATION_STEP, 0)
                            _uiState.value = _uiState.value.copy(generationStep = step)
                        }
                        else -> {
                            // Other states like BLOCKED, ENQUEUED, CANCELLED
                        }
                    }
                }
            }
        }
    }

    fun saveApiKeyAndReworkPwa(apiKey: String, uuid: String, reworkPrompt: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(apiKey)
        }
        reworkPwa(uuid, reworkPrompt)
    }

    fun getVersionHistory(uuid: String) = versionControl.getHistory(uuid)

    fun revertToVersion(uuid: String, versionId: String): Boolean {
        val success = versionControl.revertToVersion(uuid, versionId)
        if (success) {
            _uiState.value = _uiState.value.copy(
                pwaReverted = true,
                errorMessage = null
            )
        }
        return success
    }
}

data class ReworkUiState(
    val isReworking: Boolean = false,
    val pwaReworked: Boolean = false,
    val pwaReverted: Boolean = false,
    val errorMessage: String? = null,
    val generationStep: Int? = null
)
