package com.toymakerftw.mothership.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.toymakerftw.mothership.api.model.Message
import com.toymakerftw.mothership.api.model.OpenRouterRequest
import com.toymakerftw.mothership.MothershipApp
import com.toymakerftw.mothership.data.SettingsRepository
import com.toymakerftw.mothership.demo.DemoKeyManager
import org.json.JSONObject
import java.io.File
import java.net.SocketException
import java.net.UnknownHostException
import java.net.URL
import javax.net.ssl.SSLException
import kotlinx.coroutines.delay

class PwaReworkService(private val context: Context) {
    companion object {
        private const val TAG = "PwaReworkService"
        private const val MAX_RETRIES = 3
    }

    private val requiredFiles = listOf("index.html", "manifest.json", "sw.js", "app.js", "styles.css")

    suspend fun reworkPWA(uuid: String, reworkPrompt: String): Result {
        return try {
            val pwaFolder = File(context.getExternalFilesDir(null), uuid)
            if (!pwaFolder.exists() || !pwaFolder.isDirectory) {
                return Result.failure("PWA folder not found")
            }
            val files = readPwaFiles(pwaFolder)
            if (files.isEmpty()) {
                return Result.failure("PWA folder or files not found")
            }
            val apiResponse = sendToApi(reworkPrompt, files)
            if (apiResponse.isEmpty()) {
                return Result.failure("Invalid API response")
            }
            writeUpdatedFiles(pwaFolder, apiResponse)
            
            // Send a broadcast to notify that the PWA has been reworked
            val intent = Intent("com.toymakerftw.mothership.PWA_REWORKED").apply {
                putExtra("pwa_uuid", uuid)
            }
            context.sendBroadcast(intent)
            
            Result.success("PWA reworked successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Rework failed", e)
            Result.failure("Failed to rework PWA: ${e.message}")
        }
    }

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

    private suspend fun sendToApi(reworkPrompt: String, files: Map<String, String>): Map<String, String> {
        val settingsRepository = SettingsRepository(context)
        val demoKeyManager = DemoKeyManager(context)
        val apiKey = getApiKeyForRequest(settingsRepository, demoKeyManager)
            ?: throw Exception("API key not set. Please go to Settings to add your OpenRouter API key.")

        val promptBuilder = StringBuilder()
        promptBuilder.append("You are an expert UI/UX and Front-End Developer modifying an existing Progressive Web App (PWA).\n")
        promptBuilder.append("The user wants to apply the following changes: '").append(reworkPrompt).append("'\n\n")
        promptBuilder.append("Here are the existing PWA files:\n")

        for ((fileName, content) in files) {
            promptBuilder.append("--- ").append(fileName).append(" ---\n")
            promptBuilder.append(content.take(2000))
            promptBuilder.append("\n\n")
        }

        promptBuilder.append("Please provide the complete, updated PWA files in a single JSON object.")
        promptBuilder.append(" Return ONLY a JSON object with no explanations.\n")
        promptBuilder.append(" Wrap the JSON in a fenced code block as ```json ... ```.\n")
        promptBuilder.append(" The JSON should either be { \"files\": { <filename>: <content>, ... } } or a top-level object mapping filenames to their content.\n")
        promptBuilder.append(" IMPORTANT: Use TailwindCSS LOCALLY by referencing <script src='tailwind.min.js'></script> in index.html (do NOT use external CDNs). Ensure tailwind.min.js is cached by sw.js for offline use.\n")
        
        promptBuilder.append(" Use AOS LOCALLY via <link href='aos.css' rel='stylesheet'> and <script src='aos.js'></script>; initialize with <script>AOS.init();</script>. Ensure both files are cached by sw.js (no CDN).\n")
        promptBuilder.append(" Use Feather Icons LOCALLY via <script src='feather.min.js'></script> and initialize with <script>feather.replace();</script>. Ensure it is cached by sw.js (no CDN).\n")

        val messages = listOf(Message(role = "system", content = promptBuilder.toString()))
        val request = OpenRouterRequest(
            model = "x-ai/grok-4-fast:free",
            messages = messages
        )

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

    private suspend fun handleRetry(e: Exception, attempt: Int) {
        Log.w(TAG, "${e.javaClass.simpleName} on attempt $attempt: ${e.message}")
        if (attempt < MAX_RETRIES) {
            val delayMs = (2000 * attempt).toLong()
            Log.d(TAG, "Waiting ${delayMs}ms before retry $attempt")
            delay(delayMs)
        }
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

    private fun parsePwaFiles(response: String): Map<String, String> {
        return try {
            val jsonString = extractJsonFromResponse(response)
            if (jsonString.isBlank()) {
                return mapOf("index.html" to response)
            }
            val json = JSONObject(jsonString)
            val filesJson = if (json.has("files") && json.opt("files") is JSONObject) {
                json.getJSONObject("files")
            } else {
                json
            }
            val result = mutableMapOf<String, String>()
            val keys = filesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val finalKey = if (key.startsWith("files/")) key.substring(6) else key
                val value = filesJson.opt(key)
                result[finalKey] = when (value) {
                    is String -> value
                    is JSONObject -> value.toString()
                    else -> value?.toString() ?: ""
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response, falling back to simple parsing", e)
            mapOf("index.html" to response)
        }
    }

    private fun extractJsonFromResponse(response: String): String {
        // 1) Try fenced code block with explicit json language
        val fencedJsonRegex = """```json\s*([\s\S]*?)\s*```""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL))
        val fencedJsonMatch = fencedJsonRegex.find(response)
        if (fencedJsonMatch != null) {
            val extracted = fencedJsonMatch.groupValues[1].trim()
            try {
                JSONObject(extracted)
                Log.d(TAG, "Extracted valid JSON from ```json fenced block (\${extracted.length} chars)")
                return extracted
            } catch (e: org.json.JSONException) {
                Log.w(TAG, "Content in ```json block is not valid JSON; will try other methods")
            }
        }
        // 2) Try generic fenced code block ``` ... ```
        val fencedGenericRegex = """```\s*([\s\S]*?)\s*```""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL))
        val fencedGenericMatch = fencedGenericRegex.find(response)
        if (fencedGenericMatch != null) {
            val extracted = fencedGenericMatch.groupValues[1].trim()
            val start = extracted.indexOf('{')
            val end = extracted.lastIndexOf('}')
            if (start != -1 && end > start) {
                val candidate = extracted.substring(start, end + 1)
                try {
                    JSONObject(candidate)
                    Log.d(TAG, "Extracted valid JSON from generic fenced block (${candidate.length} chars)")
                    return candidate
                } catch (_: org.json.JSONException) {
                    // continue
                }
            }
        }
        // 3) Scan the whole string for a balanced top-level JSON object
        val startIndex = response.indexOf('{')
        if (startIndex == -1) {
            return ""
        }
        var openBraces = 0
        var inString = false
        var endIndex = -1
        for (i in startIndex until response.length) {
            val ch = response[i]
            if (ch == '"' && (i == 0 || response[i - 1] != '\\')) {
                inString = !inString
            }
            if (!inString) {
                when (ch) {
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
                Log.d(TAG, "Extracted valid JSON object by brace scanning")
                return potentialJson
            } catch (_: org.json.JSONException) {
                // continue
            }
        }
        // 4) Fallback: try from first '{' to last '}' if present
        val fallbackEndIndex = response.lastIndexOf('}')
        if (fallbackEndIndex > startIndex) {
            val slice = response.substring(startIndex, fallbackEndIndex + 1)
            return slice
        }
        return ""
    }

    private fun writeUpdatedFiles(pwaFolder: File, updatedFiles: Map<String, String>) {
        // Update the sw.js file to use a new cache version
        val updatedFilesWithNewSw = if (updatedFiles.containsKey("sw.js")) {
            // If sw.js is being updated, use the updated version
            val updatedSwContent = updateServiceWorkerCacheVersion(updatedFiles["sw.js"]!!)
            val mutableMap = updatedFiles.toMutableMap()
            mutableMap["sw.js"] = updatedSwContent
            mutableMap.toMap()
        } else {
            // If sw.js is not being updated, modify the existing one
            val swFile = File(pwaFolder, "sw.js")
            if (swFile.exists()) {
                val currentSwContent = swFile.readText()
                val updatedSwContent = updateServiceWorkerCacheVersion(currentSwContent)
                swFile.writeText(updatedSwContent)
            }
            updatedFiles
        }

        for ((fileName, content) in updatedFilesWithNewSw) {
            val finalFileName = if (fileName.startsWith("files/")) fileName.substring(6) else fileName
            val file = File(pwaFolder, finalFileName)
            file.writeText(content)
        }

        // Ensure local Tailwind asset exists alongside updated files
        try {
            val tailwindFile = File(pwaFolder, "tailwind.min.js")
            if (!tailwindFile.exists()) {
                val tailwindAsset = context.assets.open("tailwind.min.js")
                tailwindAsset.copyTo(tailwindFile.outputStream())
                tailwindAsset.close()
                Log.d(TAG, "Copied tailwind.min.js to PWA directory after rework")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to ensure tailwind.min.js in PWA directory", e)
        }

        

        // Ensure local AOS assets exist alongside updated files
        try {
            val aosJsFile = File(pwaFolder, "aos.js")
            if (!aosJsFile.exists()) {
                val aosJsAsset = context.assets.open("aos.js")
                aosJsAsset.copyTo(aosJsFile.outputStream())
                aosJsAsset.close()
                Log.d(TAG, "Copied aos.js to PWA directory after rework")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to ensure aos.js in PWA directory", e)
        }
        try {
            val aosCssFile = File(pwaFolder, "aos.css")
            if (!aosCssFile.exists()) {
                val aosCssAsset = context.assets.open("aos.css")
                aosCssAsset.copyTo(aosCssFile.outputStream())
                aosCssAsset.close()
                Log.d(TAG, "Copied aos.css to PWA directory after rework")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to ensure aos.css in PWA directory", e)
        }

        // Ensure local Feather Icons asset exists alongside updated files
        try {
            val featherFile = File(pwaFolder, "feather.min.js")
            if (!featherFile.exists()) {
                val featherAsset = context.assets.open("feather.min.js")
                featherAsset.copyTo(featherFile.outputStream())
                featherAsset.close()
                Log.d(TAG, "Copied feather.min.js to PWA directory after rework")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to ensure feather.min.js in PWA directory", e)
        }
    }

    private fun updateServiceWorkerCacheVersion(swContent: String): String {
        // Update the cache version in the service worker
        val cacheNameRegex = """const\s+CACHE_NAME\s*=\s*['"][^'"]*['"]""".toRegex()
        val timestamp = System.currentTimeMillis()
        val newCacheName = "const CACHE_NAME = 'pwa-cache-v$timestamp'"
        return swContent.replace(cacheNameRegex, newCacheName)
    }

    sealed class Result {
        data class Success(val message: String) : Result()
        data class Failure(val message: String) : Result()
        companion object {
            fun success(message: String): Result = Success(message)
            fun failure(message: String): Result = Failure(message)
        }
    }
}
