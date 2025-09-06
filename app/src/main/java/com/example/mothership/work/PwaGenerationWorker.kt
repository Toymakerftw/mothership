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
        // Acquire wake lock to prevent device from sleeping during network operations
        acquireWakeLock()
        
        return try {
            performWork()
        } finally {
            // Always release wake lock
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
                acquire(10 * 60 * 1000L) // 10 minutes timeout
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
                    // Wait before retrying (exponential backoff with longer delays for network issues)
                    val delayMs = (2000 * attempt).toLong() // 2s, 4s, 6s
                    Log.d("PwaGenerationWorker", "Waiting ${delayMs}ms before retry $attempt")
                    kotlinx.coroutines.delay(delayMs)
                }
            } catch (e: UnknownHostException) {
                lastException = e
                Log.w("PwaGenerationWorker", "UnknownHostException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    // Wait before retrying (exponential backoff with longer delays for network issues)
                    val delayMs = (2000 * attempt).toLong() // 2s, 4s, 6s
                    Log.d("PwaGenerationWorker", "Waiting ${delayMs}ms before retry $attempt")
                    kotlinx.coroutines.delay(delayMs)
                }
            } catch (e: SSLException) {
                lastException = e
                Log.w("PwaGenerationWorker", "SSLException on attempt $attempt: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    // Wait before retrying (exponential backoff with longer delays for network issues)
                    val delayMs = (2000 * attempt).toLong() // 2s, 4s, 6s
                    Log.d("PwaGenerationWorker", "Waiting ${delayMs}ms before retry $attempt")
                    kotlinx.coroutines.delay(delayMs)
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

        // Get the API instance
        val mothershipApp = context.applicationContext as com.example.mothership.MothershipApp
        val mothershipApi = mothershipApp.mothershipApi
        val promptRewriteService = PromptRewriteService(mothershipApi)

        // Step 1: Rewrite the prompt using mistralai model
        Log.d("PwaGenerationWorker", "Step 1: Rewriting prompt using mistralai model")
        val rewrittenPrompt = promptRewriteService.rewritePrompt(prompt, apiKey)
        Log.d("PwaGenerationWorker", "Original prompt: ${prompt.take(100)}...")
        Log.d("PwaGenerationWorker", "Rewritten prompt: ${rewrittenPrompt.take(100)}...")
        Log.d("PwaGenerationWorker", "Prompt rewriting successful: ${rewrittenPrompt != prompt}")

        // Step 2: Generate PWA using the rewritten prompt with deepseek model
        Log.d("PwaGenerationWorker", "Step 2: Generating PWA using deepseek model with rewritten prompt")
        val request = withContext(Dispatchers.IO) {
            OpenRouterRequest(
                model = "deepseek/deepseek-chat-v3.1:free",
                messages = listOf(
                    Message(
                        role = "system",
                        content = """
                        You are an expert UI/UX and Front-End Developer.  
                        You create website in a way a designer would, using ONLY HTML, CSS and Javascript.  
                        Try to create the best UI possible. Important: Make the website responsive by using TailwindCSS. Use it as much as you can, if you can't use it, use custom css (make sure to import tailwind with <script  
                         src="https://cdn.tailwindcss.com"></script> in the head).  
                        Also try to elaborate as much as you can, to create something unique, with a great design.  
                        If you want to use ICONS import Feather Icons (Make sure to add <script src="https://unpkg.com/feather-icons"></script> and <script src="https://cdn.jsdelivr.net/npm/feather-icons/dist/feather.min.js">  
                         </script> in the head., and <script>feather.replace();</script> in the body. Ex : <i data-feather="user"></i>).  
                        For scroll animations you can use: AOS.com (Make sure to add <link href="https://unpkg.com/aos@2.3.1/dist/aos.css" rel="stylesheet"> and <script src="https://unpkg.com/aos@2.3.1/dist/aos.js"></script>  
                         and <script>AOS.init();</script>).  
                        For interactive animations you can use: Vanta.js (Make sure to add <script src="https://cdn.jsdelivr.net/npm/vanta@latest/dist/vanta.globe.min.js"></script> and <script>VANTA.GLOBE({...</script> in the  
                         body.).  
                        You can create multiple pages website at once (following the format rules below) or a Single Page Application. If the user doesn't ask for a specific version, you have to determine the best version for    
                         the user, depending on the request. (Try to avoid the Single Page Application if the user asks for multiple pages.)  
                        No need to explain what you did. Just return the expected result. AVOID Chinese characters in the code if not asked by the user.  
                        Return the results in a ```html
                        ``` markdown. Format the results like:  
                        1. Start with <<<<<<< START_TITLE .  
                        2. Add the name of the page without special character, such as spaces or punctuation, using the .html format only, right after the start tag.  
                        3. Close the start tag with the  >>>>>>> END_TITLE.  
                        4. Start the HTML response with the triple backticks, like ```html.  
                        5. Insert the following html there.  
                        6. Close with the triple backticks, like
                        ```.  
                        7. Retry if another pages.  
                        Example Code:  
                        <<<<<<< START_TITLE index.html >>>>>>> END_TITLE  
                        1 <!DOCTYPE html>  
                        2 <html lang="en">  
                        3 <head>  
                        4     <meta charset="UTF-8">  
                        5     <meta name="viewport" content="width=device-width, initial-scale=1.0">  
                        6     <title>Index</title>  
                        7     <link rel="icon" type="image/x-icon" href="/static/favicon.ico">  
                        8     <script src="https://cdn.tailwindcss.com"></script>  
                        9     <link href="https://unpkg.com/aos@2.3.1/dist/aos.css" rel="stylesheet">  
                       10     <script src="https://unpkg.com/aos@2.3.1/dist/aos.js"></script>  
                       11     <script src="https://cdn.jsdelivr.net/npm/feather-icons/dist/feather.min.js"></script>  
                       12     <script src="https://cdn.jsdelivr.net/npm/animejs/lib/anime.iife.min.js"></script>  
                       13     <script src="https://unpkg.com/feather-icons"></script>  
                       14 </head>  
                       15 <body>  
                       16     <h1>Hello World</h1>  
                       17     <script>AOS.init();</script>  
                       18     <script>const { animate } = anime;</script>  
                       19     <script>feather.replace();</script>  
                       20 </body>  
                       21 </html>  
                        IMPORTANT: The first file should be always named index.html.

                        Create a complete Progressive Web App (PWA) based on this description: $rewrittenPrompt
                        
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
                    ),
                    Message(
                        role = "user", 
                        content = rewrittenPrompt
                    )
                )
            )
        }

        Log.d("PwaGenerationWorker", "Making API request with rewritten prompt")
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

        // Return success without the large files content (WorkManager has 10KB limit)
        return Result.success(
            workDataOf(
                KEY_PWA_NAME to pwaName
                // Removed KEY_RESULT_FILES to avoid WorkManager data limit exceeded
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
            Log.d("PwaGenerationWorker", "Attempting to parse JSON response...")
            
            // First, try to extract JSON from the response if it's embedded in other text
            val jsonString = extractJsonFromResponse(response)
            Log.d("PwaGenerationWorker", "Extracted JSON string length: ${jsonString.length}")
            
            // Try to parse as JSON object with files structure
            val json = org.json.JSONObject(jsonString)
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
                // If no files object, treat the whole response as index.html
                filesMap["index.html"] = response
            }

            // Ensure we have required PWA files with proper content
            if (!filesMap.containsKey("manifest.json")) {
                Log.d("PwaGenerationWorker", "Adding default manifest.json")
                filesMap["manifest.json"] = createDefaultManifest()
            } else {
                // Validate and fix manifest if needed
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
            // Fallback to simple parsing
            parseSimpleResponse(response)
        }
    }

    private fun extractJsonFromResponse(response: String): String {
        // First, try to find JSON in code blocks (```json ... ```)
        val jsonCodeBlockRegex = "```json\\s*([\\s\\S]*?)\\s*```".toRegex()
        val jsonCodeBlockMatch = jsonCodeBlockRegex.find(response)
        if (jsonCodeBlockMatch != null) {
            val extracted = jsonCodeBlockMatch.groupValues[1].trim()
            Log.d("PwaGenerationWorker", "Extracted JSON from code block (${extracted.length} chars)")
            return extracted
        }
        
        // Look for JSON object markers
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}')
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            val extracted = response.substring(jsonStart, jsonEnd + 1)
            Log.d("PwaGenerationWorker", "Extracted JSON from position $jsonStart to $jsonEnd (${extracted.length} chars)")
            return extracted
        }
        
        // If no JSON markers found, return the original response
        Log.d("PwaGenerationWorker", "No JSON markers found, using full response")
        return response
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
        Log.d("PwaGenerationWorker", "Using simple parsing fallback")
        val filesMap = mutableMapOf<String, String>()

        // First, try to extract JSON from the response if it's embedded in HTML
        val jsonString = extractJsonFromResponse(response)
        if (jsonString != response) {
            Log.d("PwaGenerationWorker", "Found embedded JSON in response, attempting to parse...")
            try {
                val json = org.json.JSONObject(jsonString)
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

        // If no JSON found or parsing failed, create a proper HTML structure
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