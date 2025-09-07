package com.example.mothership.service

import android.content.Context
import android.util.Log
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.OpenRouterRequest
import com.example.mothership.MothershipApp
import com.example.mothership.data.SettingsRepository
import com.example.mothership.demo.DemoKeyManager
import org.json.JSONObject
import java.io.File
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class PwaReworkService(private val context: Context) {
    companion object {
        private const val TAG = "PwaReworkService"
        private const val MAX_RETRIES = 3
    }

    private val requiredFiles = listOf("index.html", "manifest.json", "sw.js", "app.js", "styles.css")

    /**
     * Reworks an existing PWA by updating its files based on a rework prompt
     *
     * @param uuid The UUID of the PWA folder
     * @param reworkPrompt The prompt to guide the rework
     * @return Result object with success status and message
     */
    suspend fun reworkPWA(uuid: String, reworkPrompt: String): Result {
        return try {
            // Step 1: Locate the PWA folder using the provided UUID
            val pwaFolder = File(context.getExternalFilesDir(null), uuid)
            if (!pwaFolder.exists() || !pwaFolder.isDirectory) {
                return Result.failure("PWA folder not found")
            }

            // Step 2: Read the contents of all PWA files
            val files = readPwaFiles(pwaFolder)
            if (files.isEmpty()) {
                return Result.failure("PWA folder or files not found")
            }

            // Step 3: Send the files and rework prompt to the API
            val apiResponse = sendToApi(reworkPrompt, files)
            
            // Step 4: Receive the API response and validate it
            if (apiResponse.isEmpty()) {
                return Result.failure("Invalid API response")
            }

            // Step 5: Overwrite the existing files with the updated content
            writeUpdatedFiles(pwaFolder, apiResponse)

            // Step 6: Return success message
            Result.success("PWA reworked successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Rework failed", e)
            Result.failure("Failed to rework PWA: ${e.message}")
        }
    }

    /**
     * Reads the contents of all required PWA files
     *
     * @param pwaFolder The PWA folder to read files from
     * @return Map of filename to content
     * @throws Exception if any required file is missing
     */
    private fun readPwaFiles(pwaFolder: File): Map<String, String> {
        val files = mutableMapOf<String, String>()
        
        for (filename in requiredFiles) {
            val file = File(pwaFolder, filename)
            if (!file.exists() || !file.isFile) {
                throw Exception("Required file $filename not found")
            }
            files[filename] = file.readText()
        }
        
        return files
    }

    /**
     * Sends the files and rework prompt to the API
     *
     * @param reworkPrompt The prompt to guide the rework
     * @param files The PWA files to rework
     * @return Map of updated filename to content
     * @throws Exception if API call fails
     */
    private suspend fun sendToApi(reworkPrompt: String, files: Map<String, String>): Map<String, String> {
        val settingsRepository = SettingsRepository(context)
        val demoKeyManager = DemoKeyManager(context)
        val apiKey = getApiKeyForRequest(settingsRepository, demoKeyManager)
            ?: throw Exception("API key not set. Please go to Settings to add your OpenRouter API key.")

        // Build the prompt for the API
        val promptBuilder = StringBuilder()
        promptBuilder.append("You are an expert UI/UX and Front-End Developer modifying an existing Progressive Web App (PWA).\n")
        promptBuilder.append("The user wants to apply the following changes: '").append(reworkPrompt).append("'\n\n")
        promptBuilder.append("Here are the existing PWA files:\n")
        
        for ((fileName, content) in files) {
            promptBuilder.append("--- ").append(fileName).append(" ---\n")
            promptBuilder.append(content.take(2000)) // Limit content to avoid token limits
            promptBuilder.append("\n\n")
        }
        
        promptBuilder.append("Please provide the complete, updated PWA files in a single JSON object.")

        val messages = listOf(Message(role = "system", content = promptBuilder.toString()))
        val request = OpenRouterRequest(
            model = "qwen/qwen-2.5-coder-32b-instruct:free",
            messages = messages
        )

        // Retry logic
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                val mothershipApp = context.applicationContext as MothershipApp
                val response = mothershipApp.mothershipApi.generatePwa("Bearer $apiKey", request)

                if (settingsRepository.getApiKey().isNullOrEmpty()) {
                    demoKeyManager.incrementUsage()
                }

                if (response.choices.isEmpty()) {
                    throw Exception("No response from AI. Please try again.")
                }

                val pwaFilesContent = response.choices.first().message.content
                return parsePwaFiles(pwaFilesContent)
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

        throw Exception("Failed to rework app after $MAX_RETRIES attempts: ${lastException?.message ?: "Unknown error"}. Please check your network connection and try again.")
    }

    /**
     * Handles retry delays for API calls
     */
    private suspend fun handleRetry(e: Exception, attempt: Int) {
        Log.w(TAG, "${e.javaClass.simpleName} on attempt $attempt: ${e.message}")
        if (attempt < MAX_RETRIES) {
            val delayMs = (2000 * attempt).toLong()
            Log.d(TAG, "Waiting ${delayMs}ms before retry $attempt")
            kotlinx.coroutines.delay(delayMs)
        }
    }

    /**
     * Gets the API key for the request
     */
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

    /**
     * Parses the PWA files from the API response
     */
    private fun parsePwaFiles(response: String): Map<String, String> {
        return try {
            val jsonString = extractJsonFromResponse(response)
            val json = JSONObject(jsonString)
            val filesJson = json.getJSONObject("files")
            val result = mutableMapOf<String, String>()
            val keys = filesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = filesJson.getString(key)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response, falling back to simple parsing", e)
            mapOf("index.html" to response) // Fallback for non-json response
        }
    }

    /**
     * Extracts JSON from the API response
     */
    private fun extractJsonFromResponse(response: String): String {
        // First, try to find JSON in code blocks (```json ... ```)
        val jsonCodeBlockRegex = """json\s*([\s\S]*?)\s*""".toRegex()
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

    /**
     * Writes the updated files to the PWA folder
     *
     * @param pwaFolder The PWA folder to write files to
     * @param updatedFiles The updated files to write
     * @throws Exception if file writing fails
     */
    private fun writeUpdatedFiles(pwaFolder: File, updatedFiles: Map<String, String>) {
        for ((fileName, content) in updatedFiles) {
            val file = File(pwaFolder, fileName)
            file.writeText(content)
        }
    }

    /**
     * Result class to encapsulate success/failure status
     */
    sealed class Result {
        data class Success(val message: String) : Result()
        data class Failure(val message: String) : Result()

        companion object {
            fun success(message: String): Result = Success(message)
            fun failure(message: String): Result = Failure(message)
        }
    }
}