
package com.example.mothership.work

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mothership.MothershipApp
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.OpenRouterRequest
import com.example.mothership.data.SettingsRepository
import com.example.mothership.demo.DemoKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class PwaReworkWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PROMPT = "prompt"
        const val KEY_PWA_UUID = "pwa_uuid"
        const val KEY_PWA_NAME = "pwa_name"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val MAX_RETRIES = 3
        private const val TAG = "PwaReworkWorker"
    }

    private val notificationHelper = PwaNotificationHelper(context)
    private var wakeLock: PowerManager.WakeLock? = null

    override suspend fun doWork(): Result {
        acquireWakeLock()
        return try {
            performWork()
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
                acquire(10 * 60 * 1000L) // 10 minutes timeout
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

    private suspend fun performWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT) ?: return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "No rework prompt provided")
        )
        val pwaUuid = inputData.getString(KEY_PWA_UUID) ?: return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "No PWA UUID provided")
        )
        val pwaName = inputData.getString(KEY_PWA_NAME) ?: "PWA"

        notificationHelper.showProgressNotification("Reworking $pwaName")

        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                val result = performPwaRework(prompt, pwaUuid, pwaName)
                notificationHelper.cancelProgressNotification()
                if (result is Result.Success) {
                    notificationHelper.showSuccessNotification("Reworked $pwaName")
                }
                return result
            } catch (e: SocketException) {
                lastException = e
                handleRetry(e, attempt)
            } catch (e: UnknownHostException) {
                lastException = e
                handleRetry(e, attempt)
            } catch (e: SSLException) {
                lastException = e
                handleRetry(e, attempt)
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Non-retryable exception on attempt $attempt", e)
                break
            }
        }

        notificationHelper.cancelProgressNotification()
        val errorMessage = "Failed to rework app after $MAX_RETRIES attempts: ${lastException?.message ?: "Unknown error"}. Please check your network connection and try again."
        notificationHelper.showErrorNotification("Reworking $pwaName", errorMessage)
        return Result.failure(workDataOf(KEY_ERROR_MESSAGE to errorMessage))
    }

    private suspend fun handleRetry(e: Exception, attempt: Int) {
        Log.w(TAG, "${e.javaClass.simpleName} on attempt $attempt: ${e.message}")
        if (attempt < MAX_RETRIES) {
            val delayMs = (2000 * attempt).toLong()
            Log.d(TAG, "Waiting ${delayMs}ms before retry $attempt")
            kotlinx.coroutines.delay(delayMs)
        }
    }

    private suspend fun performPwaRework(prompt: String, pwaUuid: String, pwaName: String): Result {
        val settingsRepository = SettingsRepository(context)
        val demoKeyManager = DemoKeyManager(context)
        val apiKey = getApiKeyForRequest(settingsRepository, demoKeyManager)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "API key not set. Please go to Settings to add your OpenRouter API key."))

        if (prompt.isBlank()) {
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Please enter a description for reworking your app."))
        }

        val existingFiles = readExistingPwaFiles(pwaUuid)
        if (existingFiles.isEmpty()) {
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "PWA directory not found or empty."))
        }

        val reworkPrompt = buildReworkPrompt(prompt, existingFiles)
        val request = createOpenRouterRequest(reworkPrompt)

        return try {
            val mothershipApp = context.applicationContext as MothershipApp
            val response = mothershipApp.mothershipApi.generatePwa("Bearer $apiKey", request)

            if (settingsRepository.getApiKey().isNullOrEmpty()) {
                demoKeyManager.incrementUsage()
            }

            if (response.choices.isEmpty()) {
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "No response from AI. Please try again."))
            }

            val pwaFilesContent = response.choices.first().message.content
            savePwaFiles(pwaUuid, pwaName, pwaFilesContent)

            Result.success(workDataOf(KEY_PWA_NAME to pwaName, KEY_PWA_UUID to pwaUuid))
        } catch (e: retrofit2.HttpException) {
            val errorMessage = when (e.code()) {
                429 -> "Rate limit exceeded. Please try again later or add your own API key in Settings."
                408 -> "Request timeout. Please check your network connection and try again."
                else -> "HTTP error ${e.code()}: ${e.message()}. Please try again."
            }
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during rework", e)
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to "An unexpected error occurred: ${e.message ?: "Unknown error"}. Please try again."))
        }
    }

    private fun readExistingPwaFiles(pwaUuid: String): Map<String, String> {
        val pwaDir = File(context.getExternalFilesDir(null), pwaUuid)
        if (!pwaDir.exists() || !pwaDir.isDirectory) {
            return emptyMap()
        }
        return pwaDir.listFiles()?.filter { it.isFile }?.associate {
            it.name to it.readText()
        } ?: emptyMap()
    }

    private fun buildReworkPrompt(userPrompt: String, existingFiles: Map<String, String>): String {
        return buildString {
            append("You are an expert UI/UX and Front-End Developer modifying an existing Progressive Web App (PWA).\n")
            append("The user wants to apply the following changes: '$userPrompt'\n\n")
            append("Here are the existing PWA files:\n")
            existingFiles.forEach { (fileName, content) ->
                append("---" to fileName)
                append(content.take(2000)) // Limit content to avoid token limits
                append("\n\n")
            }
            append("Please provide the complete, updated PWA files in a single JSON object.")
        }
    }

    private suspend fun createOpenRouterRequest(reworkPrompt: String): OpenRouterRequest {
        return withContext(Dispatchers.IO) {
            OpenRouterRequest(
                model = "moonshotai/kimi-dev-72b:free",
                messages = listOf(
                    Message(role = "system", content = getReworkSystemPrompt()),
                    Message(role = "user", content = reworkPrompt)
                )
            )
        }
    }

    private fun getReworkSystemPrompt(): String {
        return """
            You are an expert UI/UX and Front-End Developer. Your task is to modify an existing Progressive Web App (PWA) based on the user's request.
            You will be provided with the user's request and the content of the existing PWA files.
            Your response must be a single JSON object containing all the PWA files, including the modified ones and any new ones.
            The JSON object must have a "files" key, which is an object where the keys are the filenames and the values are the file contents.
            For example:
            {
              "files": {
                "index.html": "<!DOCTYPE html>...",
                "styles.css": "body { ... }",
                "app.js": "console.log('hello');",
                "manifest.json": "{...}",
                "sw.js": "..."
              }
            }
            Ensure that the generated PWA is complete and functional.
        """.trimIndent()
    }

    private suspend fun getApiKeyForRequest(
        settingsRepository: SettingsRepository,
        demoKeyManager: DemoKeyManager
    ): String? {
        val userApiKey = settingsRepository.getApiKey()
        if (!userApiKey.isNullOrEmpty()) {
            return userApiKey
        }
        if (demoKeyManager.canUseDemoKey()) {
            var demoApiKey = demoKeyManager.getDemoApiKey()
            if (demoApiKey.isNullOrEmpty()) {
                demoKeyManager.registerDevice()
                demoApiKey = demoKeyManager.fetchDemoApiKey()
            }
            return demoApiKey
        }
        return null
    }

    private fun savePwaFiles(pwaUuid: String, pwaName: String, filesContent: String) {
        val pwaDir = File(context.getExternalFilesDir(null), pwaUuid)
        if (!pwaDir.exists()) {
            pwaDir.mkdirs()
        }

        val appInfo = """{"name": "$pwaName", "uuid": "$pwaUuid"} """
        File(pwaDir, "app_info.json").writeText(appInfo)

        val filesMap = parsePwaFiles(filesContent)

        filesMap.forEach { (fileName, content) ->
            File(pwaDir, fileName).writeText(content)
        }
    }

    private fun parsePwaFiles(response: String): Map<String, String> {
        return try {
            val jsonString = extractJsonFromResponse(response)
            val json = JSONObject(jsonString)
            val filesJson = json.getJSONObject("files")
            filesJson.keys().asSequence().associateWith { filesJson.getString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response, falling back to simple parsing", e)
            mapOf("index.html" to response) // Fallback for non-json response
        }
    }

    private fun extractJsonFromResponse(response: String): String {
                val jsonCodeBlockRegex = """```json\s*([\s\S]*?)\s*```""".toRegex()
        val match = jsonCodeBlockRegex.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        val start = response.indexOf('{')
        val end = response.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1)
        }
        return response
    }
}
