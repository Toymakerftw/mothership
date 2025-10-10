package com.toymakerftw.appsage

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.toymakerftw.appsage.data.SettingsRepository
import com.toymakerftw.appsage.versioncontrol.VersionControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ReworkViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReworkUiState())
    val uiState: StateFlow<ReworkUiState> = _uiState

    private val versionControl = VersionControl(context)
    private val workManager = WorkManager.getInstance(context)
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
        workManager.getWorkInfoByIdLiveData(workId).observeForever { workInfo ->
            if (workInfo != null) {
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.value = _uiState.value.copy(
                            isReworking = false,
                            pwaReworked = true,
                            generationStep = null
                        )
                        workManager.getWorkInfoByIdLiveData(workId).removeObserver { }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(PwaReworkWorker.KEY_ERROR_MESSAGE)
                        _uiState.value = _uiState.value.copy(
                            isReworking = false,
                            errorMessage = error,
                            generationStep = null
                        )
                        workManager.getWorkInfoByIdLiveData(workId).removeObserver { }
                    }
                    WorkInfo.State.RUNNING -> {
                        val step = workInfo.progress.getInt(PwaReworkWorker.KEY_GENERATION_STEP, 0)
                        _uiState.value = _uiState.value.copy(generationStep = step)
                    }
                    else -> {
                        // Other states
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
