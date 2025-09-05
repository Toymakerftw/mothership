package com.example.mothership.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PwaWorkManager(private val context: Context) {

    fun enqueuePwaGeneration(prompt: String, pwaName: String = "PWA"): UUID {
        val workId = UUID.randomUUID().toString()

        val inputData = workDataOf(
            PwaGenerationWorker.KEY_PROMPT to prompt,
            PwaGenerationWorker.KEY_PWA_NAME to pwaName
        )

        // Define constraints - require network connectivity
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create work request with retry policy
        val workRequest = OneTimeWorkRequestBuilder<PwaGenerationWorker>()
            .setId(UUID.fromString(workId))
            .setInputData(inputData)
            .setConstraints(constraints)
            .keepResultsForAtLeast(java.time.Duration.ofMinutes(30)) // Keep results for 30 minutes
            .build()

        // Enqueue the work
        WorkManager.getInstance(context)
            .beginUniqueWork(workId, ExistingWorkPolicy.KEEP, workRequest)
            .enqueue()

        return UUID.fromString(workId)
    }

    fun getWorkInfo(workId: UUID): Flow<WorkInfo?> {
        return WorkManager.getInstance(context)
            .getWorkInfoByIdFlow(workId)
    }

    fun getWorkState(workId: UUID): Flow<WorkInfo.State?> {
        return getWorkInfo(workId).map { it?.state }
    }

    fun getWorkOutputData(workId: UUID): Flow<Data?> {
        return getWorkInfo(workId).map { it?.outputData }
    }

    fun cancelWork(workId: UUID) {
        WorkManager.getInstance(context).cancelWorkById(workId)
    }

    companion object {
        @Volatile
        private var INSTANCE: PwaWorkManager? = null

        fun getInstance(context: Context): PwaWorkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PwaWorkManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}