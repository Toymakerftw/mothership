package com.example.mothership.work

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mothership.api.MothershipApi
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.OpenRouterRequest
import com.example.mothership.data.SettingsRepository
import com.example.mothership.demo.DemoKeyManager
import com.example.mothership.service.PromptRewriteService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONException
import java.io.File
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class PwaGenerationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PROMPT = "prompt"
        const val KEY_PWA_NAME = "pwa_name"
        const val KEY_RESULT_FILES = "result_files"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val MAX_RETRIES = 3
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
                "Mothership::PwaGenerationWorker"
            ).apply {
                acquire(10 * 60 * 1000L)
            }
            Log.d("PwaGenerationWorker", "Wake lock acquired")
        } catch (e: Exception) {
            Log.e("PwaGenerationWorker", "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("PwaGenerationWorker", "Wake lock released")
                }
            }
        } catch (e: Exception) {
            Log.e("PwaGenerationWorker", "Failed to release wake lock", e)
        }
    }

    private suspend fun performWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT) ?: return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "No prompt provided")
        )
        val pwaName = inputData.getString(KEY_PWA_NAME) ?: "PWA"

        notificationHelper.showProgressNotification(pwaName)
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                val result = performPwaGeneration(prompt, pwaName)
                notificationHelper.cancelProgressNotification()
                if (result is Result.Success) {
                    notificationHelper.showSuccessNotification(pwaName)
                }
                return result
            } catch (e: SocketException) {
                lastException = e
                Log.w("PwaGenerationWorker", "SocketException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    val delayMs = (2000 * attempt).toLong()
                    Log.d("PwaGenerationWorker", "Waiting ${delayMs}ms before retry $attempt")
                    kotlinx.coroutines.delay(delayMs)
                }
            } catch (e: UnknownHostException) {
                lastException = e
                Log.w("PwaGenerationWorker", "UnknownHostException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    val delayMs = (2000 * attempt).toLong()
                    Log.d("PwaGenerationWorker", "Waiting ${delayMs}ms before retry $attempt")
                    kotlinx.coroutines.delay(delayMs)
                }
            } catch (e: SSLException) {
                lastException = e
                Log.w("PwaGenerationWorker", "SSLException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    val delayMs = (2000 * attempt).toLong()
                    Log.d("PwaGenerationWorker", "Waiting ${delayMs}ms before retry $attempt")
                    kotlinx.coroutines.delay(delayMs)
                }
            } catch (e: Exception) {
                lastException = e
                Log.e("PwaGenerationWorker", "Non-retryable exception on attempt $attempt", e)
                break
            }
        }

        notificationHelper.cancelProgressNotification()
        notificationHelper.showErrorNotification(
            pwaName,
            "Failed to generate app after $MAX_RETRIES attempts: ${lastException?.message ?: "Unknown error"}. Please check your network connection and try again."
        )

        return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "Failed to generate app after $MAX_RETRIES attempts: ${lastException?.message ?: "Unknown error"}. Please check your network connection and try again.")
        )
    }

    private fun getSystemPrompt(prompt: String): String {
        val promptBuilder = com.example.mothership.service.JsonPromptBuilder()
        return promptBuilder.buildPwaGenerationPrompt(prompt)
    }

    private suspend fun performPwaGeneration(prompt: String, pwaName: String): Result {
        val settingsRepository = SettingsRepository(context)
        val demoKeyManager = DemoKeyManager(context)
        val apiKey = getApiKeyForRequest(settingsRepository, demoKeyManager)

        if (apiKey.isNullOrEmpty()) {
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "API key not set. Please go to Settings to add your OpenRouter API key.")
            )
        }
        if (prompt.isBlank()) {
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Please enter a description for your app.")
            )
        }

        val mothershipApp = context.applicationContext as com.example.mothership.MothershipApp
        val mothershipApi = mothershipApp.mothershipApi
        val promptRewriteService = PromptRewriteService(mothershipApi)

        Log.d("PwaGenerationWorker", "Step 1: Rewriting prompt using mistralai model")
        val rewrittenPrompt = promptRewriteService.rewritePrompt(prompt, apiKey)
        Log.d("PwaGenerationWorker", "Original prompt: ${prompt.take(100)}...")
        Log.d("PwaGenerationWorker", "Rewritten prompt: ${rewrittenPrompt.take(100)}...")
        Log.d("PwaGenerationWorker", "Prompt rewriting successful: ${rewrittenPrompt != prompt}")

        Log.d("PwaGenerationWorker", "Step 2: Generating PWA using qwen/qwen-2.5-coder-32b-instruct:free model with rewritten prompt")
        val request = withContext(Dispatchers.IO) {
            OpenRouterRequest(
                model = "qwen/qwen-2.5-coder-32b-instruct:free",
                messages = listOf(
                    Message(
                        role = "system",
                        content = getSystemPrompt(rewrittenPrompt)
                    )
                )
            )
        }

        Log.d("PwaGenerationWorker", "Making API request with rewritten prompt")
        val response = mothershipApi.generatePwa("Bearer $apiKey", request)
        Log.d("PwaGenerationWorker", "Received API response successfully")

        if (settingsRepository.getApiKey().isNullOrEmpty()) {
            demoKeyManager.incrementUsage()
        }

        if (response.choices.isEmpty()) {
            Log.e("PwaGenerationWorker", "No response from AI or empty choices")
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "No response from AI. Please try again.")
            )
        }

        val pwaFiles = response.choices.first().message.content
        Log.d("PwaGenerationWorker", "Received PWA files content length: ${pwaFiles.length}")
        savePwaFiles(pwaName, pwaFiles)

        return Result.success(
            workDataOf(
                KEY_PWA_NAME to pwaName
            )
        )
    }

    private suspend fun getApiKeyForRequest(
        settingsRepository: SettingsRepository,
        demoKeyManager: DemoKeyManager
    ): String? {
        Log.d("PwaGenerationWorker", "Getting API key for request")
        val userApiKey = settingsRepository.getApiKey()
        if (!userApiKey.isNullOrEmpty()) {
            Log.d("PwaGenerationWorker", "Using user-provided API key")
            return userApiKey
        }

        Log.d("PwaGenerationWorker", "No user API key found, checking demo key")
        if (!demoKeyManager.canUseDemoKey()) {
            Log.d("PwaGenerationWorker", "Demo limit reached")
            return null
        }

        Log.d("PwaGenerationWorker", "Demo limit not reached, fetching demo key")
        var demoApiKey = demoKeyManager.getDemoApiKey()
        if (demoApiKey.isNullOrEmpty()) {
            Log.d("PwaGenerationWorker", "No existing demo key found, registering device and fetching new one")
            demoKeyManager.clearDeviceId()
            demoKeyManager.registerDevice()
            demoApiKey = demoKeyManager.fetchDemoApiKey()
            if (demoApiKey.isNullOrEmpty()) {
                Log.d("PwaGenerationWorker", "Failed to fetch demo API key")
                return null
            }
        }

        Log.d("PwaGenerationWorker", "Using demo API key")
        return demoApiKey
    }

    private fun savePwaFiles(pwaName: String, files: String) {
        val uuid = java.util.UUID.randomUUID().toString()
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        pwaDir.mkdirs()
        val appInfo = "{\"name\": \"$pwaName\", \"uuid\": \"$uuid\"}"
        val appInfoFile = File(pwaDir, "app_info.json")
        appInfoFile.writeText(appInfo)

        val filesMap = try {
            parseJsonResponse(files)
        } catch (e: Exception) {
            parseSimpleResponse(files)
        }

        filesMap.entries.chunked(3).forEach { chunk ->
            chunk.forEach { (fileName, content) ->
                val file = File(pwaDir, fileName)
                file.writeText(content)
            }
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }

        try {
            val faviconAsset = context.assets.open("favicon.ico")
            val faviconFile = File(pwaDir, "favicon.ico")
            faviconAsset.copyTo(faviconFile.outputStream())
            faviconAsset.close()
            Log.d("PwaGenerationWorker", "Copied favicon.ico to PWA directory")
        } catch (e: Exception) {
            Log.w("PwaGenerationWorker", "Failed to copy favicon.ico to PWA directory", e)
        }
    }

    private fun parseJsonResponse(response: String): Map<String, String> {
        return try {
            Log.d("PwaGenerationWorker", "Attempting to parse JSON response...")
            val jsonString = extractJsonFromResponse(response)
            Log.d("PwaGenerationWorker", "Extracted JSON string length: ${jsonString.length}")
            val json = JSONObject(jsonString)
            val filesMap = mutableMapOf<String, String>()

            if (json.has("files")) {
                val filesJson = json.getJSONObject("files")
                Log.d("PwaGenerationWorker", "Found 'files' object with ${filesJson.length()} files")
                filesJson.keys().forEach { key ->
                    filesMap[key] = filesJson.getString(key)
                    Log.d("PwaGenerationWorker", "Extracted file: $key (${filesMap[key]?.length ?: 0} chars)")
                }
            } else {
                Log.w("PwaGenerationWorker", "No 'files' object found in JSON, treating as index.html")
                filesMap["index.html"] = response
            }

            if (!filesMap.containsKey("manifest.json")) {
                Log.d("PwaGenerationWorker", "Adding default manifest.json")
                filesMap["manifest.json"] = createDefaultManifest()
            } else {
                val manifestContent = filesMap["manifest.json"] ?: "{}"
                filesMap["manifest.json"] = fixManifest(manifestContent)
            }

            if (!filesMap.containsKey("sw.js")) {
                Log.d("PwaGenerationWorker", "Adding default service worker")
                filesMap["sw.js"] = """
                    self.addEventListener('fetch', event => {
                        // Simple service worker
                    });
                """.trimIndent()
            }

            Log.d("PwaGenerationWorker", "Successfully parsed ${filesMap.size} files from JSON")
            filesMap
        } catch (e: Exception) {
            Log.e("PwaGenerationWorker", "JSON parsing failed: ${e.message}", e)
            parseSimpleResponse(response)
        }
    }

    private fun extractJsonFromResponse(response: String): String {
        val jsonCodeBlockRegex = """/```json\s*([\s\S]*?)\s*```""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val jsonCodeBlockMatch = jsonCodeBlockRegex.find(response)
        if (jsonCodeBlockMatch != null) {
            val extracted = jsonCodeBlockMatch.groupValues[1].trim()
            Log.d("PwaGenerationWorker", "Extracted JSON from code block (${extracted.length} chars)")
            try {
                JSONObject(extracted)
                return extracted
            } catch (e: JSONException) {
                Log.w("PwaGenerationWorker", "Extracted content from code block is not valid JSON. Falling back.")
            }
        }

        val startIndex = response.indexOf('{')
        if (startIndex == -1) {
            Log.d("PwaGenerationWorker", "No JSON object found in response.")
            return ""
        }

        val endIndex = response.lastIndexOf('}')
        if (endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1)
        }

        Log.d("PwaGenerationWorker", "Could not extract valid JSON. Returning empty string.")
        return ""
    }

    private fun createDefaultManifest(): String {
        return """
            {
              "name": "Generated PWA",
              "short_name": "PWA",
              "start_url": "index.html",
              "display": "standalone",
              "background_color": "#ffffff",
              "theme_color": "#000000",
              "icons": []
            }
        """.trimIndent()
    }

    private fun fixManifest(manifestContent: String): String {
        return try {
            val manifestJson = JSONObject(manifestContent)
            if (!manifestJson.has("name")) {
                manifestJson.put("name", "Generated PWA")
            }
            if (!manifestJson.has("short_name")) {
                manifestJson.put("short_name", manifestJson.optString("name", "PWA"))
            }
            if (!manifestJson.has("start_url")) {
                manifestJson.put("start_url", "index.html")
            }
            if (!manifestJson.has("display")) {
                manifestJson.put("display", "standalone")
            }
            if (!manifestJson.has("background_color")) {
                manifestJson.put("background_color", "#ffffff")
            }
            if (!manifestJson.has("theme_color")) {
                manifestJson.put("theme_color", "#000000")
            }
            manifestJson.toString(2)
        } catch (e: Exception) {
            createDefaultManifest()
        }
    }

    private fun parseSimpleResponse(response: String): Map<String, String> {
        Log.d("PwaGenerationWorker", "Using simple parsing fallback")
        val filesMap = mutableMapOf<String, String>()
        val jsonString = extractJsonFromResponse(response)
        if (jsonString != response) {
            Log.d("PwaGenerationWorker", "Found embedded JSON in response, attempting to parse...")
            try {
                val json = JSONObject(jsonString)
                if (json.has("files")) {
                    val filesJson = json.getJSONObject("files")
                    filesJson.keys().forEach { key ->
                        filesMap[key] = filesJson.getString(key)
                        Log.d("PwaGenerationWorker", "Extracted file from embedded JSON: $key")
                    }
                    Log.d("PwaGenerationWorker", "Successfully parsed embedded JSON with ${filesMap.size} files")
                    return filesMap
                }
            } catch (e: Exception) {
                Log.w("PwaGenerationWorker", "Failed to parse embedded JSON: ${e.message}")
            }
        }

        Log.d("PwaGenerationWorker", "Creating fallback HTML structure")
        filesMap["index.html"] = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Generated PWA</title>
                <link rel="manifest" href="manifest.json">
                <link rel="stylesheet" href="styles.css">
                <link rel="icon" href="favicon.ico" type="image/x-icon">
            </head>
            <body>
                <div id="app">
                    <h1>Generated PWA</h1>
                    <div id="content">$response</div>
                </div>
                <script src="app.js"></script>
                <script>
                    if ('serviceWorker' in navigator) {
                        window.addEventListener('load', function() {
                            navigator.serviceWorker.register('sw.js')
                                .then(function(registration) {
                                    console.log('SW registered: ', registration);
                                })
                                .catch(function(registrationError) {
                                    console.log('SW registration failed: ', registrationError);
                                });
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        filesMap["manifest.json"] = createDefaultManifest()
        filesMap["sw.js"] = """
            self.addEventListener('fetch', event => {
                event.respondWith(
                    caches.match(event.request)
                        .then(response => {
                            return response || fetch(event.request);
                        })
                );
            });

            self.addEventListener('install', event => {
                console.log('Service Worker installing.');
            });

            self.addEventListener('activate', event => {
                console.log('Service Worker activating.');
            });
        """.trimIndent()

        filesMap["styles.css"] = """
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                margin: 0;
                padding: 20px;
                background-color: #f5f5f5;
            }

            #app {
                max-width: 800px;
                margin: 0 auto;
                background: white;
                padding: 20px;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }

            h1 {
                color: #333;
                border-bottom: 1px solid #eee;
                padding-bottom: 10px;
            }

            #content {
                margin-top: 20px;
                line-height: 1.6;
            }
        """.trimIndent()

        filesMap["app.js"] = """
            console.log('PWA App loaded');
            document.addEventListener('DOMContentLoaded', function() {
                console.log('DOM fully loaded and parsed');
            });
        """.trimIndent()

        return filesMap
    }
}
