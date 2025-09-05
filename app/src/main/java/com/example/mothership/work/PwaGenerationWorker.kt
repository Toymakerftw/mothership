package com.example.mothership.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mothership.api.MothershipApi
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.OpenRouterRequest
import com.example.mothership.data.SettingsRepository
import com.example.mothership.demo.DemoKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun doWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT) ?: return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "No prompt provided")
        )

        val pwaName = inputData.getString(KEY_PWA_NAME) ?: "PWA"
        
        // Show progress notification
        notificationHelper.showProgressNotification(pwaName)

        // Try up to MAX_RETRIES times
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                val result = performPwaGeneration(prompt, pwaName)
                // Cancel progress notification and show result
                notificationHelper.cancelProgressNotification()
                if (result is Result.Success) {
                    notificationHelper.showSuccessNotification(pwaName)
                }
                return result
            } catch (e: SocketException) {
                lastException = e
                Log.w("PwaGenerationWorker", "SocketException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    // Wait before retrying (exponential backoff)
                    kotlinx.coroutines.delay((1000 * attempt).toLong())
                }
            } catch (e: UnknownHostException) {
                lastException = e
                Log.w("PwaGenerationWorker", "UnknownHostException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    // Wait before retrying (exponential backoff)
                    kotlinx.coroutines.delay((1000 * attempt).toLong())
                }
            } catch (e: SSLException) {
                lastException = e
                Log.w("PwaGenerationWorker", "SSLException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    // Wait before retrying (exponential backoff)
                    kotlinx.coroutines.delay((1000 * attempt).toLong())
                }
            } catch (e: Exception) {
                // For other exceptions, don't retry
                lastException = e
                Log.e("PwaGenerationWorker", "Non-retryable exception on attempt $attempt", e)
                break
            }
        }

        // If we get here, all retries failed
        notificationHelper.cancelProgressNotification()
        notificationHelper.showErrorNotification(
            pwaName, 
            "Failed to generate app after $MAX_RETRIES attempts: ${lastException?.message ?: "Unknown error"}. Please check your network connection and try again."
        )
        
        return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "Failed to generate app after $MAX_RETRIES attempts: ${lastException?.message ?: "Unknown error"}. Please check your network connection and try again.")
        )
    }

    private suspend fun performPwaGeneration(prompt: String, pwaName: String): Result {
        // Get the API key - first try user key, then demo key
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

        // Create the API request
        val request = withContext(Dispatchers.IO) {
            OpenRouterRequest(
                model = "deepseek/deepseek-chat-v3.1:free",
                messages = listOf(
                    Message(
                        role = "user", content = """
                        Create a complete Progressive Web App (PWA) based on this description: $prompt
                        
                        Requirements:
                        1. Return ONLY valid JSON with a "files" object containing:
                           - "index.html": Complete HTML file with proper structure
                           - "manifest.json": Valid PWA manifest file
                           - "sw.js": Basic service worker
                           - "app.js": JavaScript file for functionality
                           - "styles.css": CSS styling file
                        
                        2. The index.html must:
                           - Reference manifest.json in the head
                           - Reference app.js and styles.css
                           - Register sw.js as service worker
                           - Be a complete, valid HTML document
                        
                        3. The manifest.json must be a valid PWA manifest with proper start_url
                        
                        4. The sw.js must be a basic but functional service worker
                        
                        5. The app.js should contain the main functionality described
                        
                        6. The styles.css should contain all necessary styling
                        
                        Return ONLY the JSON object with the files, nothing else.
                        
                        Example format:
                        {
                          "files": {
                            "index.html": "<!DOCTYPE html>...",
                            "manifest.json": "{...}",
                            "sw.js": "...",
                            "app.js": "...",
                            "styles.css": "..."
                          }
                        }
                    """.trimIndent()
                    )
                )
            )
        }

        // Get the API instance
        val mothershipApp = context.applicationContext as com.example.mothership.MothershipApp
        val mothershipApi = mothershipApp.mothershipApi

        Log.d("PwaGenerationWorker", "Making API request with request: $request")
        val response = mothershipApi.generatePwa("Bearer $apiKey", request)
        Log.d("PwaGenerationWorker", "Received API response successfully")

        // Increment usage if we used a demo key
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

        // Save the files
        savePwaFiles(pwaName, pwaFiles)

        // Return success with the generated files
        return Result.success(
            workDataOf(
                KEY_PWA_NAME to pwaName,
                KEY_RESULT_FILES to pwaFiles
            )
        )
    }

    /**
     * Gets the appropriate API key for the request:
     * 1. User-provided key (if exists)
     * 2. Demo key (if available and under limit)
     */
    private suspend fun getApiKeyForRequest(
        settingsRepository: SettingsRepository,
        demoKeyManager: DemoKeyManager
    ): String? {
        Log.d("PwaGenerationWorker", "Getting API key for request")

        // First, try the user-provided API key
        val userApiKey = settingsRepository.getApiKey()
        if (!userApiKey.isNullOrEmpty()) {
            Log.d("PwaGenerationWorker", "Using user-provided API key")
            return userApiKey
        }

        Log.d("PwaGenerationWorker", "No user API key found, checking demo key")

        // If no user key, check if we can use demo key
        if (!demoKeyManager.canUseDemoKey()) {
            Log.d("PwaGenerationWorker", "Demo limit reached")
            return null
        }

        Log.d("PwaGenerationWorker", "Demo limit not reached, fetching demo key")

        // Try to get existing demo key or fetch a new one
        var demoApiKey = demoKeyManager.getDemoApiKey()
        if (demoApiKey.isNullOrEmpty()) {
            Log.d("PwaGenerationWorker", "No existing demo key found, registering device and fetching new one")
            // Clear existing device ID to force fresh registration
            demoKeyManager.clearDeviceId()
            // Register device
            demoKeyManager.registerDevice()
            // Fetch a new demo API key
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

        // Try to parse the response as JSON first
        val filesMap = try {
            parseJsonResponse(files)
        } catch (e: Exception) {
            // If JSON parsing fails, fallback to simple parsing
            parseSimpleResponse(files)
        }

        // Write files with reduced memory pressure and minimal UI updates
        filesMap.entries.chunked(3).forEach { chunk ->
            chunk.forEach { (fileName, content) ->
                val file = File(pwaDir, fileName)
                file.writeText(content)
            }
            // Small delay between file operations to reduce memory pressure and CPU usage
            try {
                Thread.sleep(50) // Increased delay to reduce CPU usage
            } catch (e: InterruptedException) {
                // Handle interruption gracefully
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    private fun parseJsonResponse(response: String): Map<String, String> {
        return try {
            // Try to parse as JSON object with files structure
            val json = org.json.JSONObject(response)
            val filesMap = mutableMapOf<String, String>()

            if (json.has("files")) {
                val filesJson = json.getJSONObject("files")
                filesJson.keys().forEach { key ->
                    filesMap[key] = filesJson.getString(key)
                }
            } else {
                // If no files object, treat the whole response as index.html
                filesMap["index.html"] = response
            }

            // Ensure we have required PWA files with proper content
            if (!filesMap.containsKey("manifest.json")) {
                filesMap["manifest.json"] = createDefaultManifest()
            } else {
                // Validate and fix manifest if needed
                val manifestContent = filesMap["manifest.json"] ?: "{}"
                filesMap["manifest.json"] = fixManifest(manifestContent)
            }

            if (!filesMap.containsKey("sw.js")) {
                filesMap["sw.js"] = """
                    self.addEventListener('fetch', event => {
                        // Simple service worker
                    });
                """.trimIndent()
            }

            filesMap
        } catch (e: Exception) {
            // Fallback to simple parsing
            parseSimpleResponse(response)
        }
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
            val manifestJson = org.json.JSONObject(manifestContent)

            // Ensure required fields are present
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
            // If we can't fix it, return a default manifest
            createDefaultManifest()
        }
    }

    private fun parseSimpleResponse(response: String): Map<String, String> {
        // Simple parsing: try to extract HTML, CSS, JS if possible
        val filesMap = mutableMapOf<String, String>()

        // Create a proper HTML structure
        filesMap["index.html"] = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Generated PWA</title>
                <link rel="manifest" href="manifest.json">
                <link rel="stylesheet" href="styles.css">
            </head>
            <body>
                <div id="app">
                    <h1>Generated PWA</h1>
                    <div id="content">$response</div>
                </div>
                <script src="app.js"></script>
                <script>
                    // Register service worker
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
                // Simple service worker
                event.respondWith(
                    caches.match(event.request)
                        .then(response => {
                            // Return cached version or fetch from network
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
            // Generated JavaScript file
            console.log('PWA App loaded');
            
            // Add any basic interactivity here
            document.addEventListener('DOMContentLoaded', function() {
                console.log('DOM fully loaded and parsed');
            });
        """.trimIndent()

        return filesMap
    }
}