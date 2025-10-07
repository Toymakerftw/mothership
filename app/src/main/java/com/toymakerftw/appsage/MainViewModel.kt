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
import java.util.UUID
import java.io.File
import org.json.JSONObject

class MainViewModel(
    private val context: Context,
    private val appsageApi: AppsageApi,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())
    val models: StateFlow<List<ModelInfo>> = _models

    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel

    init {
        loadModels()
    }

    fun generatePwa(prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)
            
            try {
                val apiKey = settingsRepository.getApiKey()
                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(isGenerating = false, errorMessage = "API key not set")
                    return@launch
                }

                val selectedModelId = _selectedModel.value ?: "openai/gpt-3.5-turbo"
                
                val request = OpenRouterRequest(
                    model = selectedModelId,
                    messages = listOf(
                        Message(
                            role = "user",
                            content = "Generate a complete PWA with HTML, CSS, and JavaScript code in JSON format. The PWA should implement: $prompt. Include index.html, style.css, and script.js in the JSON response."
                        )
                    )
                )
                
                val response = appsageApi.generatePwa("Bearer $apiKey", request)
                
                if (response.choices.isNotEmpty()) {
                    val content = response.choices[0].message.content
                    extractAndSavePwaCode(content)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating PWA", e)
                _uiState.value = _uiState.value.copy(isGenerating = false, errorMessage = e.message)
            }
        }
    }

    private fun extractAndSavePwaCode(responseContent: String) {
        try {
            // Try to parse the response as JSON
            val jsonResponse = JSONObject(responseContent)
            
            // Extract code files
            val htmlContent = jsonResponse.optString("index.html", "")
            val cssContent = jsonResponse.optString("style.css", "")
            val jsContent = jsonResponse.optString("script.js", "")
            
            // If the response is not in expected JSON format, try to extract code from text
            if (htmlContent.isEmpty() && cssContent.isEmpty() && jsContent.isEmpty()) {
                // Look for code blocks in the response
                val uuid = UUID.randomUUID().toString()
                val pwaDir = File(context.getExternalFilesDir(null), uuid)
                pwaDir.mkdirs()
                
                // Save the raw response for now
                val responseFile = File(pwaDir, "raw_response.txt")
                responseFile.writeText(responseContent)
                
                // Parse for code blocks from the text response
                val htmlMatch = Regex("```html\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
                val cssMatch = Regex("```css\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
                val jsMatch = Regex("```javascript\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent) 
                    ?: Regex("```js\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
                
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
                
                // Create a basic manifest.json file
                val manifestContent = """{
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
                val manifestFile = File(pwaDir, "manifest.json")
                manifestFile.writeText(manifestContent)
                
                // Create a basic app_info.json
                val appInfoFile = File(pwaDir, "app_info.json")
                appInfoFile.writeText("""{"name": "Generated PWA", "uuid": "$uuid"}""")
                
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    pwaGenerated = true,
                    pwaUuid = uuid
                )
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
                
                // Create a basic manifest.json file
                val manifestContent = """{
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
                val manifestFile = File(pwaDir, "manifest.json")
                manifestFile.writeText(manifestContent)
                
                // Create a basic app_info.json
                val appInfoFile = File(pwaDir, "app_info.json")
                appInfoFile.writeText("""{"name": "Generated PWA", "uuid": "$uuid"}""")
                
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    pwaGenerated = true,
                    pwaUuid = uuid
                )
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error extracting PWA code", e)
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                errorMessage = "Error extracting PWA code: ${e.message}"
            )
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            try {
                val apiKey = settingsRepository.getApiKey()
                if (!apiKey.isNullOrEmpty()) {
                    val response = appsageApi.getModels("Bearer $apiKey")
                    val modelList = response.data.map { model ->
                        ModelInfo(model.id, model.name, model.description ?: "")
                    }
                    _models.value = modelList
                    if (modelList.isNotEmpty()) {
                        _selectedModel.value = modelList[0].id
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading models", e)
                // Use a default model if loading fails
                _selectedModel.value = "openai/gpt-3.5-turbo"
            }
        }
    }

    fun setSelectedModel(modelId: String) {
        _selectedModel.value = modelId
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
        }
    }

    fun getPwas(): List<Pair<String, String>> {
        val pwaDir = context.getExternalFilesDir(null)
        if (pwaDir != null && pwaDir.exists()) {
            return pwaDir.listFiles()?.mapNotNull { 
                if (it.isDirectory) {
                    val appInfoFile = File(it, "app_info.json")
                    val manifestFile = File(it, "manifest.json")
                    
                    if (appInfoFile.exists()) {
                        try {
                            // Try to get name from manifest.json first (prefer short_name)
                            var pwaName = "Untitled App"
                            
                            if (manifestFile.exists()) {
                                try {
                                    val manifestContent = manifestFile.readText()
                                    val manifestJson = org.json.JSONObject(manifestContent)
                                    
                                    // Prefer short_name, fallback to name, then to app_info name
                                    pwaName = manifestJson.optString("short_name") 
                                        ?: manifestJson.optString("name") 
                                        ?: "Untitled App"
                                } catch (manifestException: Exception) {
                                    // If manifest parsing fails, fall back to app_info
                                }
                            }
                            
                            // If we still don't have a good name, try app_info.json
                            if (pwaName == "Untitled App") {
                                val appInfo = appInfoFile.readText()
                                val jsonObject = org.json.JSONObject(appInfo)
                                pwaName = jsonObject.optString("name", "Untitled App")
                            }
                            
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
        return emptyList()
    }
}

data class MainUiState(
    val isGenerating: Boolean = false,
    val pwaGenerated: Boolean = false,
    val pwaUuid: String? = null,
    val errorMessage: String? = null
)

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String
)