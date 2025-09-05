package com.example.mothership

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mothership.api.MothershipApi
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.OpenRouterRequest
import com.example.mothership.data.SettingsRepository
import com.example.mothership.demo.DemoKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.System

class MainViewModel(private val mothershipApi: MothershipApi, private val settingsRepository: SettingsRepository, private val context: Context) : ViewModel() {

    private val demoKeyManager = DemoKeyManager(context)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _pwas = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pwas = _pwas.asStateFlow()

    // Prompt state for main screen
    private val _prompt = MutableStateFlow("")
    val prompt = _prompt.asStateFlow()

    fun setPrompt(value: String) {
        _prompt.value = value
    }

    fun clearPrompt() {
        _prompt.value = ""
    }

    fun generatePwa(prompt: String) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                // Start the foreground service to keep the app alive during generation
                PwaGenerationService.startService(context, prompt)
                
                // Get the API key - first try user key, then demo key
                val apiKey = getApiKeyForRequest()
                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = MainUiState.Error("API key not set. Please go to Settings to add your OpenRouter API key.")
                    // Notify service of completion with error
                    notifyServiceCompletion(prompt, false, "API key not set")
                    return@launch
                }

                if (prompt.isBlank()) {
                    _uiState.value = MainUiState.Error("Please enter a description for your app.")
                    // Notify service of completion with error
                    notifyServiceCompletion(prompt, false, "Please enter a description for your app")
                    return@launch
                }

                // Move heavy operations to background thread
                val request = withContext(Dispatchers.IO) {
                    OpenRouterRequest(
                        model = "deepseek/deepseek-chat-v3.1:free",
                        messages = listOf(Message(role = "user", content = """
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
                        """.trimIndent()))
                    )
                }

                Log.d("MainViewModel", "Making API request with request: $request")
                val response = mothershipApi.generatePwa("Bearer $apiKey", request)
                Log.d("MainViewModel", "Received API response successfully")
                
                // Increment usage if we used a demo key
                if (settingsRepository.getApiKey().isNullOrEmpty()) {
                    demoKeyManager.incrementUsage()
                }
                
                if (response.choices.isEmpty()) {
                    Log.e("MainViewModel", "No response from AI or empty choices")
                    _uiState.value = MainUiState.Error("No response from AI. Please try again.")
                    notifyServiceCompletion(prompt, false, "No response from AI")
                    return@launch
                }

                val pwaFiles = response.choices.first().message.content
                Log.d("MainViewModel", "Received PWA files content length: ${pwaFiles.length}")

                // Perform file operations in background thread
                withContext(Dispatchers.IO) {
                    savePwaFiles(prompt, pwaFiles)
                }

                _uiState.value = MainUiState.Success
                // Trigger garbage collection to reduce memory pressure
                System.gc()
                // Notify service of successful completion
                notifyServiceCompletion(prompt, true)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception in generatePwa", e)
                _uiState.value = MainUiState.Error("Failed to generate app: ${e.message ?: "Unknown error"}. Please try again.")
                // Notify service of completion with error
                notifyServiceCompletion(prompt, false, e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Gets the appropriate API key for the request:
     * 1. User-provided key (if exists)
     * 2. Demo key (if available and under limit)
     */
    private suspend fun getApiKeyForRequest(): String? {
        Log.d("MainViewModel", "Getting API key for request")
        
        // First, try the user-provided API key
        val userApiKey = settingsRepository.getApiKey()
        if (!userApiKey.isNullOrEmpty()) {
            Log.d("MainViewModel", "Using user-provided API key")
            return userApiKey
        }
        
        Log.d("MainViewModel", "No user API key found, checking demo key")
        
        // If no user key, check if we can use demo key
        if (!demoKeyManager.canUseDemoKey()) {
            Log.d("MainViewModel", "Demo limit reached")
            _uiState.value = MainUiState.Error("Demo limit reached (5 calls/day). Please add your own OpenRouter API key in Settings to continue using Mothership.")
            return null
        }
        
        Log.d("MainViewModel", "Demo limit not reached, fetching demo key")
        
        // Try to get existing demo key or fetch a new one
        var demoApiKey = demoKeyManager.getDemoApiKey()
        if (demoApiKey.isNullOrEmpty()) {
            Log.d("MainViewModel", "No existing demo key found, registering device and fetching new one")
            // Clear existing device ID to force fresh registration
            demoKeyManager.clearDeviceId()
            // Register device
            demoKeyManager.registerDevice()
            // Fetch a new demo API key
            demoApiKey = demoKeyManager.fetchDemoApiKey()
            if (demoApiKey.isNullOrEmpty()) {
                Log.d("MainViewModel", "Failed to fetch demo API key")
                _uiState.value = MainUiState.Error("Failed to fetch demo API key. Please add your own OpenRouter API key in Settings.")
                return null
            }
        }
        
        Log.d("MainViewModel", "Using demo API key")
        return demoApiKey
    }
    
    private fun notifyServiceCompletion(pwaName: String, success: Boolean, errorMessage: String? = null) {
        val intent = Intent(context, PwaGenerationService::class.java).apply {
            action = "NOTIFY_COMPLETION"
            putExtra(PwaGenerationService.EXTRA_PWA_NAME, pwaName)
            putExtra(PwaGenerationService.EXTRA_SUCCESS, success)
            putExtra(PwaGenerationService.EXTRA_ERROR_MESSAGE, errorMessage)
        }
        context.startService(intent)
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

    fun deletePwa(uuid: String) {
        viewModelScope.launch {
            val pwaDir = File(context.getExternalFilesDir(null), uuid)
            if (pwaDir.exists()) {
                // First uninstall from desktop
                val installer = PwaInstaller(context)
                installer.uninstall(uuid)
                
                // Then delete the files
                pwaDir.deleteRecursively()
                getPwas()
            }
        }
    }

    fun getPwas() {
        viewModelScope.launch {
            try {
                val pwaDir = context.getExternalFilesDir(null)
                if (pwaDir != null && pwaDir.exists()) {
                    _pwas.value = pwaDir.listFiles()?.mapNotNull { 
                        if (it.isDirectory) {
                            val appInfoFile = File(it, "app_info.json")
                            if (appInfoFile.exists()) {
                                try {
                                    val appInfo = appInfoFile.readText()
                                    val jsonObject = org.json.JSONObject(appInfo)
                                    val pwaName = jsonObject.optString("name", "Untitled App")
                                    it.name to pwaName
                                } catch (e: Exception) {
                                    // Handle corrupted app_info.json files
                                    null
                                }
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } ?: emptyList()
                }
            } catch (e: Exception) {
                _pwas.value = emptyList()
            }
        }
    }
}

sealed class MainUiState {
    object Idle : MainUiState()
    object Loading : MainUiState()
    object Success : MainUiState()
    data class Error(val message: String) : MainUiState()
}