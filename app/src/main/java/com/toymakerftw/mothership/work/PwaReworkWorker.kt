
package com.toymakerftw.mothership.work

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.toymakerftw.mothership.service.PwaReworkService

class PwaReworkWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PROMPT = "prompt"
        const val KEY_PWA_UUID = "pwa_uuid"
        const val KEY_PWA_NAME = "pwa_name"
        const val KEY_ERROR_MESSAGE = "error_message"
        private const val TAG = "PwaReworkWorker"
        private const val WAKE_LOCK_TIMEOUT = 10 * 60 * 1000L // 10 minutes
    }

    private val notificationHelper = PwaNotificationHelper(context)
    private var wakeLock: PowerManager.WakeLock? = null

    override suspend fun doWork(): Result {
        acquireWakeLock()
        try {
            val prompt = inputData.getString(KEY_PROMPT) ?: return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "No rework prompt provided")
            )
            val pwaUuid = inputData.getString(KEY_PWA_UUID) ?: return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "No PWA UUID provided")
            )
            val pwaName = inputData.getString(KEY_PWA_NAME) ?: "PWA"

            notificationHelper.showProgressNotification("Reworking $pwaName")

            // Use the refactored PwaReworkService
            val reworkService = PwaReworkService(context)
            val reworkResult = reworkService.reworkPWA(pwaUuid, prompt)

            notificationHelper.cancelProgressNotification()
            
            return when (reworkResult) {
                is PwaReworkService.Result.Success -> {
                    notificationHelper.showSuccessNotification("Reworked $pwaName")
                    Result.success(workDataOf(KEY_PWA_NAME to pwaName, KEY_PWA_UUID to pwaUuid))
                }
                is PwaReworkService.Result.Failure -> {
                    notificationHelper.showErrorNotification("Reworking $pwaName", reworkResult.message)
                    Result.failure(workDataOf(KEY_ERROR_MESSAGE to reworkResult.message))
                }
            }
        } finally {
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Mothership::PwaReworkWorker"
            ).apply {
                acquire(WAKE_LOCK_TIMEOUT)
            }
            Log.d(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }
}
