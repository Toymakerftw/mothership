package com.toymakerftw.appsage

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toymakerftw.appsage.api.AppsageApi
import com.toymakerftw.appsage.api.Message
import com.toymakerftw.appsage.api.OpenRouterRequest
import com.toymakerftw.appsage.data.SettingsRepository
import com.toymakerftw.appsage.service.PwaManager
import com.toymakerftw.appsage.PwaInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID
import java.io.File
import java.io.EOFException
import org.json.JSONObject

class MainViewModel(
    private val context: Context,
    private val appsageApi: AppsageApi,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel

    init {
        // Set default model instead of loading models
        _selectedModel.value = "x-ai/grok-4-fast"
        
        // Load initial API key
        loadApiKey()
    }
    
    private fun loadApiKey() {
        viewModelScope.launch {
            val apiKey = settingsRepository.getApiKey()
            _uiState.value = _uiState.value.copy(apiKey = apiKey)
        }
    }

    fun generatePwa(prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true, 
                errorMessage = null,
                generationStep = 0,
                pwaGenerated = false
            )
            
            try {
                val apiKey = settingsRepository.getApiKey()
                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false, 
                        errorMessage = "API key not set",
                        generationStep = null
                    )
                    return@launch
                }

                // Step 0: Analyzing prompt
                delay(500) // Brief delay for UI feedback
                _uiState.value = _uiState.value.copy(generationStep = 0)

                val selectedModelId = _selectedModel.value ?: "x-ai/grok-4-fast"
                
                val request = OpenRouterRequest(
                    model = selectedModelId,
                    messages = listOf(
                        Message(
                            role = "user",
                            content = """Generate a complete PWA with HTML, CSS, and JavaScript code in JSON format. The PWA should implement: $prompt. Include index.html, style.css, and script.js in the JSON response. Also include a manifest.json file in the response. If the response is in JSON format, include these files at the top level of the JSON object. The manifest.json should include the proper name and short_name based on the prompt. For example:
{
  "index.html": "<!DOCTYPE html>...",
  "style.css": "body { ... }",
  "script.js": "console.log('...');",
  "manifest.json": "{\\"name\\": \\"My PWA App\\", \\"short_name\\": \\"PWA App\\"}"
}"""
                        )
                    )
                )
                
                // Step 1: Generating code
                _uiState.value = _uiState.value.copy(generationStep = 1)
                
                val response = try {
                    appsageApi.generatePwa("Bearer $apiKey", request)
                } catch (e: EOFException) {
                    Log.e("MainViewModel", "EOFException during API call - response likely truncated", e)
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generationStep = null,
                        errorMessage = "API response was incomplete. Please try again later."
                    )
                    return@launch
                }
                
                // Step 2: Styling UI
                _uiState.value = _uiState.value.copy(generationStep = 2)
                delay(300) // Brief delay for UI feedback
                
                if (response.choices.isNotEmpty()) {
                    val content = response.choices[0].message.content
                    
                    // Step 3: Finalizing
                    _uiState.value = _uiState.value.copy(generationStep = 3)
                    
                    extractAndSavePwaCode(content)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating PWA", e)
                // Check if it's specifically an EOFException for better error messaging
                if (e is EOFException || e.cause is EOFException) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generationStep = null,
                        errorMessage = "API response was incomplete. This may be due to a network timeout or connection issue. Please try again."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generationStep = null,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    private fun extractAndSavePwaCode(responseContent: String) {
        try {
            // Try to parse the response as JSON, but handle incomplete responses gracefully
            val jsonResponse = try {
                JSONObject(responseContent.trim())
            } catch (jsonException: Exception) {
                // If JSON parsing fails, try to extract code from text blocks
                Log.w("MainViewModel", "Could not parse response as JSON, trying code blocks", jsonException)
                extractFromCodeBlocks(responseContent)
                return
            }
            
            // Extract code files
            val htmlContent = jsonResponse.optString("index.html", "")
            val cssContent = jsonResponse.optString("style.css", "")
            val jsContent = jsonResponse.optString("script.js", "")
            val manifestContent = jsonResponse.optString("manifest.json", "")
            
            // If the response is not in expected JSON format, try to extract code from text
            if (htmlContent.isEmpty() && cssContent.isEmpty() && jsContent.isEmpty()) {
                // Look for code blocks in the response
                extractFromCodeBlocks(responseContent)
            } else {
                // Handle expected JSON format
                val uuid = UUID.randomUUID().toString()
                val pwaDir = File(context.getExternalFilesDir(null), uuid)
                pwaDir.mkdirs()
                
                if (htmlContent.isNotEmpty()) {
                    val htmlFile = File(pwaDir, "index.html")
                    htmlFile.writeText(htmlContent)
                }
                
                if (cssContent.isNotEmpty()) {
                    val cssFile = File(pwaDir, "style.css")
                    cssFile.writeText(cssContent)
                }
                
                if (jsContent.isNotEmpty()) {
                    val jsFile = File(pwaDir, "script.js")
                    jsFile.writeText(jsContent)
                }
                
                // Create manifest.json file if provided, otherwise create a basic one
                val finalManifestContent = if (manifestContent.isNotEmpty()) {
                    manifestContent
                } else {
                    """{
    "name": "Generated PWA",
    "short_name": "PWAGen",
    "start_url": "/index.html",
    "display": "standalone",
    "background_color": "#ffffff",
    "theme_color": "#000000",
    "icons": [
        {
            "src": "/icon.png",
            "sizes": "192x192",
            "type": "image/png"
        }
    ]
}"""
                }
                
                val manifestFile = File(pwaDir, "manifest.json")
                manifestFile.writeText(finalManifestContent)
                
                // Extract name from manifest for app_info.json
                var pwaName = "Generated PWA"
                try {
                    val manifestJson = JSONObject(finalManifestContent)
                    val shortName = manifestJson.optString("short_name")
                    val manifestName = manifestJson.optString("name")
                    
                    // Prefer short_name, fallback to name from manifest
                    val betterName = shortName.ifEmpty { manifestName }
                    if (betterName.isNotEmpty()) {
                        pwaName = betterName
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Could not extract name from manifest", e)
                }
                
                // Create a basic app_info.json with the extracted name
                val appInfoFile = File(pwaDir, "app_info.json")
                appInfoFile.writeText("""{"name": "$pwaName", "uuid": "$uuid"}""")
                
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generationStep = null,
                    pwaGenerated = true,
                    pwaUuid = uuid
                )
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error extracting PWA code", e)
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                generationStep = null,
                errorMessage = "Error extracting PWA code: ${e.message}"
            )
        }
    }

    private fun extractFromCodeBlocks(responseContent: String) {
        val uuid = UUID.randomUUID().toString()
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        pwaDir.mkdirs()
        
        // Save the raw response for debugging
        val responseFile = File(pwaDir, "raw_response.txt")
        responseFile.writeText(responseContent)
        
        // Parse for code blocks from the text response
        val htmlMatch = Regex("```html\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val cssMatch = Regex("```css\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val jsMatch = Regex("```javascript\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent) 
            ?: Regex("```js\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        
        // Extract manifest from code blocks if available
        val manifestMatch = Regex("```json\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        
        if (htmlMatch != null) {
            val htmlCode = htmlMatch.groupValues[1]
            val htmlFile = File(pwaDir, "index.html")
            htmlFile.writeText(htmlCode)
        }
        
        if (cssMatch != null) {
            val cssCode = cssMatch.groupValues[1]
            val cssFile = File(pwaDir, "style.css")
            cssFile.writeText(cssCode)
        }
        
        if (jsMatch != null) {
            val jsCode = jsMatch.groupValues[1]
            val jsFile = File(pwaDir, "script.js")
            jsFile.writeText(jsCode)
        }
        
        // Create manifest.json file - try to get from code blocks first, then create a basic one
        var finalManifestContent = """{
    "name": "Generated PWA",
    "short_name": "PWAGen",
    "start_url": "/index.html",
    "display": "standalone",
    "background_color": "#ffffff",
    "theme_color": "#000000",
    "icons": [
        {
            "src": "/icon.png",
            "sizes": "192x192",
            "type": "image/png"
        }
    ]
}"""
        
        if (manifestMatch != null) {
            val manifestCode = manifestMatch.groupValues[1]
            // Try to parse the manifest to extract name
            try {
                val manifestJson = JSONObject(manifestCode)
                val manifestName = manifestJson.optString("name", "Generated PWA")
                // Update manifest with proper name
                finalManifestContent = manifestCode
            } catch (e: Exception) {
                Log.w("MainViewModel", "Could not parse manifest from code blocks", e)
            }
        }
        
        val manifestFile = File(pwaDir, "manifest.json")
        manifestFile.writeText(finalManifestContent)
        
        // Extract name from manifest for app_info.json
        var pwaName = "Generated PWA"
        try {
            val manifestJson = JSONObject(finalManifestContent)
            val shortName = manifestJson.optString("short_name")
            val manifestName = manifestJson.optString("name")
            
            // Prefer short_name, fallback to name from manifest
            val betterName = shortName.ifEmpty { manifestName }
            if (betterName.isNotEmpty()) {
                pwaName = betterName
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "Could not extract name from manifest", e)
        }
        
        // Create a basic app_info.json with the extracted name
        val appInfoFile = File(pwaDir, "app_info.json")
        appInfoFile.writeText("""{"name": "$pwaName", "uuid": "$uuid"}""")
        
        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            generationStep = null,
            pwaGenerated = true,
            pwaUuid = uuid
        )
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun saveApiKeyAndGeneratePwa(apiKey: String, prompt: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(apiKey)
        }
        generatePwa(prompt)
    }
    
    fun deletePwa(uuid: String) {
        viewModelScope.launch {
            val pwaManager = PwaManager(context)
            pwaManager.deletePwa(uuid)
            _uiState.value = _uiState.value.copy(pwaDeleted = true)
        }
    }

    fun clearPwaDeleted() {
        _uiState.value = _uiState.value.copy(pwaDeleted = false)
    }

    fun getPwas(): List<Pair<String, String>> {
        val pwaDir = context.getExternalFilesDir(null)
        if (pwaDir != null && pwaDir.exists()) {
            return pwaDir.listFiles()?.mapNotNull { 
                if (it.isDirectory) {
                    val appInfoFile = File(it, "app_info.json")
                    val manifestFile = File(it, "manifest.json")
                    
                    if (appInfoFile.exists() || manifestFile.exists()) {
                        try {
                            // Try to get name from manifest.json first (prefer short_name)
                            var pwaName = "Untitled App"
                            
                            if (manifestFile.exists()) {
                                try {
                                    val manifestContent = manifestFile.readText()
                                    val manifestJson = org.json.JSONObject(manifestContent)
                                    
                                    // Prefer short_name, fallback to name, then to app_info name
                                    val shortName = manifestJson.optString("short_name")
                                    val manifestName = manifestJson.optString("name")
                                    
                                    // Prefer short_name, fallback to name
                                    val betterName = shortName.ifEmpty { manifestName }
                                    if (betterName.isNotEmpty()) {
                                        pwaName = betterName
                                    }
                                } catch (manifestException: Exception) {
                                    Log.w("MainViewModel", "Could not parse manifest.json for ${it.name}", manifestException)
                                }
                            }
                            
                            // If we still don't have a good name, try app_info.json
                            if (pwaName == "Untitled App" && appInfoFile.exists()) {
                                val appInfo = appInfoFile.readText()
                                try {
                                    val jsonObject = org.json.JSONObject(appInfo)
                                    val appInfoName = jsonObject.optString("name", "Untitled App")
                                    if (appInfoName != "Untitled App") {
                                        pwaName = appInfoName
                                    }
                                } catch (e: Exception) {
                                    Log.w("MainViewModel", "Could not parse app_info.json for ${it.name}", e)
                                }
                            }
                            
                            it.name to pwaName
                        } catch (e: Exception) {
                            Log.w("MainViewModel", "Error getting PWA name for ${it.name}", e)
                            // Handle corrupted json files
                            it.name to "Untitled App (${it.name})"
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            } ?: emptyList()
        }
        return emptyList()
    }
}

data class MainUiState(
    val isGenerating: Boolean = false,
    val generationStep: Int? = null,
    val pwaGenerated: Boolean = false,
    val pwaUuid: String? = null,
    val errorMessage: String? = null,
    val apiKey: String? = null,
    val pwaDeleted: Boolean = false
)