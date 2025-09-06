
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

        val reworkPrompt = buildString {
            append("You are an expert UI/UX and Front-End Developer modifying an existing Progressive Web App (PWA).\n")
            append("The user wants to apply the following changes: '$prompt'\n\n")
            append("Here are the existing PWA files:\n")
            existingFiles.forEach { (fileName, content) ->
                append("---" to fileName)
                append(content.take(2000)) // Limit content to avoid token limits
                append("\n\n")
            }
            append("Please provide the complete, updated PWA files in a single JSON object.")
        }
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

    private suspend fun createOpenRouterRequest(reworkPrompt: String): OpenRouterRequest {
        return withContext(Dispatchers.IO) {
            OpenRouterRequest(
                model = "qwen/qwen-2.5-coder-32b-instruct:free",
                messages = listOf(
                    Message(role = "system", content = getSystemPrompt(reworkPrompt))
                )
            )
        }
    }

    private fun getSystemPrompt(prompt: String): String {
        val promptBuilder = com.example.mothership.service.JsonPromptBuilder()
        return promptBuilder.buildPwaReworkPrompt(prompt)
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

        val appInfo = """{"name": "$pwaName", "uuid": "$pwaUuid"} """.trimIndent()
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
        // First, try to find JSON in code blocks (```json ... ```)
        val jsonCodeBlockRegex = "```json\\s*([\\s\\S]*?)\\s*```".toRegex()
        val jsonCodeBlockMatch = jsonCodeBlockRegex.find(response)
        if (jsonCodeBlockMatch != null) {
            val extracted = jsonCodeBlockMatch.groupValues[1].trim()
            try {
                JSONObject(extracted)
                Log.d(TAG, "Extracted valid JSON from code block.")
                return extracted
            } catch (e: org.json.JSONException) {
                Log.w(TAG, "Extracted content from code block is not valid JSON.")
            }
        }

        val startIndex = response.indexOf('{')
        if (startIndex == -1) {
            return response // No json found
        }

        var openBraces = 0
        var inString = false
        var endIndex = -1

        for (i in startIndex until response.length) {
            val char = response[i]

            if (char == '"' && (i == 0 || response[i - 1] != '\\')) {
                inString = !inString
            }

            if (!inString) {
                when (char) {
                    '{' -> openBraces++
                    '}' -> openBraces--
                }
            }

            if (openBraces == 0) {
                endIndex = i
                break
            }
        }

        if (endIndex != -1) {
            val potentialJson = response.substring(startIndex, endIndex + 1)
            try {
                JSONObject(potentialJson)
                Log.d(TAG, "Extracted valid JSON object.")
                return potentialJson
            } catch (e: org.json.JSONException) {
                Log.w(TAG, "Could not parse extracted text as JSON, falling back to old method.")
            }
        }

        // Fallback to original implementation
        val fallbackEndIndex = response.lastIndexOf('}')
        if (fallbackEndIndex > startIndex) {
            return response.substring(startIndex, fallbackEndIndex + 1)
        }

        return response
    }
}
