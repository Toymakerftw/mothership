
package com.example.mothership

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mothership.api.MothershipApi
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.OpenRouterRequest
import com.example.mothership.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(private val mothershipApi: MothershipApi, private val settingsRepository: SettingsRepository, private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _pwas = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pwas = _pwas.asStateFlow()

    fun generatePwa(prompt: String) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                val apiKey = settingsRepository.getApiKey()
                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = MainUiState.Error("API key not set. Please go to Settings to add your OpenRouter API key.")
                    return@launch
                }

                if (prompt.isBlank()) {
                    _uiState.value = MainUiState.Error("Please enter a description for your app.")
                    return@launch
                }

                val request = OpenRouterRequest(
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
                        
                        3. The manifest.json must be a valid PWA manifest
                        
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

                val response = mothershipApi.generatePwa("Bearer $apiKey", request)

                if (response.choices.isEmpty()) {
                    _uiState.value = MainUiState.Error("No response from AI. Please try again.")
                    return@launch
                }

                val pwaFiles = response.choices.first().message.content

                savePwaFiles(prompt, pwaFiles)

                _uiState.value = MainUiState.Success
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Failed to generate app: ${e.message ?: "Unknown error"}. Please try again.")
            }
        }
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

        filesMap.forEach { (fileName, content) ->
            val file = File(pwaDir, fileName)
            file.writeText(content)
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
            
            // Ensure we have required PWA files
            if (!filesMap.containsKey("manifest.json")) {
                filesMap["manifest.json"] = """
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
        
        filesMap["manifest.json"] = """
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
