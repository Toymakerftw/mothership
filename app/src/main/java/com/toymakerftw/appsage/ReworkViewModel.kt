package com.toymakerftw.appsage

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toymakerftw.appsage.api.AppsageApi
import com.toymakerftw.appsage.api.Message
import com.toymakerftw.appsage.api.OpenRouterRequest
import com.toymakerftw.appsage.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import org.json.JSONObject

class ReworkViewModel(
    private val context: Context,
    private val appsageApi: AppsageApi,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReworkUiState())
    val uiState: StateFlow<ReworkUiState> = _uiState

    fun reworkPwa(uuid: String, reworkPrompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReworking = true, errorMessage = null)
            
            try {
                val apiKey = settingsRepository.getApiKey()
                if (apiKey.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(isReworking = false, errorMessage = "API key not set")
                    return@launch
                }

                val pwaDir = File(context.getExternalFilesDir(null), uuid)
                if (!pwaDir.exists() || !pwaDir.isDirectory) {
                    _uiState.value = _uiState.value.copy(isReworking = false, errorMessage = "PWA directory not found")
                    return@launch
                }

                // Read existing files
                val filesToRead = listOf("index.html", "style.css", "script.js", "manifest.json")
                val fileContents = mutableMapOf<String, String>()
                
                filesToRead.forEach { fileName ->
                    val file = File(pwaDir, fileName)
                    if (file.exists()) {
                        fileContents[fileName] = file.readText()
                    }
                }

                if (fileContents.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isReworking = false, errorMessage = "No files to rework")
                    return@launch
                }

                // Create a prompt with the current files and the rework request
                val fileContentStr = fileContents.entries.joinToString("\n\n") { (fileName, content) ->
                    "### $fileName ###\n```\n$content\n```\n"
                }

                val fullPrompt = """
                    You are given the following files from a PWA. Please modify them based on the user's rework request.
                    
                    Current files:
                    $fileContentStr
                    
                    User's rework request: $reworkPrompt
                    
                    Return the updated files in JSON format with keys for "index.html", "style.css", "script.js", and other files as needed.
                """.trimIndent()

                val request = OpenRouterRequest(
                    model = "x-ai/grok-4-fast", // Using default model for rework
                    messages = listOf(
                        Message(
                            role = "user",
                            content = fullPrompt
                        )
                    )
                )
                
                val response = appsageApi.generatePwa("Bearer $apiKey", request)
                
                if (response.choices.isNotEmpty()) {
                    val content = response.choices[0].message.content
                    updatePwaCode(uuid, content, fileContents)
                }
            } catch (e: Exception) {
                Log.e("ReworkViewModel", "Error reworking PWA", e)
                _uiState.value = _uiState.value.copy(isReworking = false, errorMessage = e.message)
            }
        }
    }

    private fun updatePwaCode(uuid: String, responseContent: String, originalFiles: Map<String, String>) {
        try {
            val pwaDir = File(context.getExternalFilesDir(null), uuid)
            if (!pwaDir.exists()) return

            // Try to parse the response as JSON
            val jsonResponse = JSONObject(responseContent)
            
            // Get the keys from the JSON response
            val keys = jsonResponse.keys()
            
            var updated = false
            for (key in keys) {
                if (jsonResponse.get(key) is String) {
                    val content = jsonResponse.getString(key)
                    val file = File(pwaDir, key)
                    file.writeText(content)
                    updated = true
                }
            }
            
            if (updated) {
                _uiState.value = _uiState.value.copy(
                    isReworking = false,
                    pwaReworked = true
                )
            } else {
                // If JSON parsing didn't work, try extracting from code blocks
                val htmlMatch = Regex("```html\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
                val cssMatch = Regex("```css\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
                val jsMatch = Regex("```javascript\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent) 
                    ?: Regex("```js\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
                
                if (htmlMatch != null) {
                    val htmlCode = htmlMatch.groupValues[1]
                    val htmlFile = File(pwaDir, "index.html")
                    htmlFile.writeText(htmlCode)
                    updated = true
                }
                
                if (cssMatch != null) {
                    val cssCode = cssMatch.groupValues[1]
                    val cssFile = File(pwaDir, "style.css")
                    cssFile.writeText(cssCode)
                    updated = true
                }
                
                if (jsMatch != null) {
                    val jsCode = jsMatch.groupValues[1]
                    val jsFile = File(pwaDir, "script.js")
                    jsFile.writeText(jsCode)
                    updated = true
                }
                
                if (updated) {
                    _uiState.value = _uiState.value.copy(
                        isReworking = false,
                        pwaReworked = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isReworking = false,
                        errorMessage = "Could not parse the response for rework"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ReworkViewModel", "Error updating PWA code", e)
            _uiState.value = _uiState.value.copy(
                isReworking = false,
                errorMessage = "Error updating PWA code: ${e.message}"
            )
        }
    }
    
    fun saveApiKeyAndReworkPwa(apiKey: String, uuid: String, reworkPrompt: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(apiKey)
        }
        reworkPwa(uuid, reworkPrompt)
    }
}

data class ReworkUiState(
    val isReworking: Boolean = false,
    val pwaReworked: Boolean = false,
    val errorMessage: String? = null
)