package com.example.mothership

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mothership.api.AppInfo
import com.example.mothership.api.Message
import com.example.mothership.api.MothershipApi
import com.example.mothership.api.OpenRouterChatRequest
import com.example.mothership.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

class MainViewModel(private val api: MothershipApi, private val context: Context) : ViewModel() {

    private val settingsRepository = SettingsRepository(context)
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    fun getApps() {
        viewModelScope.launch {
            try {
                // For now, we'll return an empty list since we're not implementing local storage yet
                _apps.value = emptyList()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching apps", e)
            }
        }
    }

    fun sendMessage(prompt: String) {
        viewModelScope.launch {
            try {
                // Add user message
                val userMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    text = prompt,
                    isUser = true
                )
                _chatMessages.value = _chatMessages.value + userMessage

                // Set generating state
                _isGenerating.value = true
                _progress.value = 0f

                // Update progress for each step
                updateProgress(0.2f, "Analyzing your request...")
                
                // Get API key from settings
                val apiKey = settingsRepository.getOpenRouterApiKey()
                if (apiKey.isNullOrEmpty()) {
                    throw Exception("OpenRouter API key not set. Please go to Settings to add your API key.")
                }

                // Step 1: Optimize Prompt
                updateProgress(0.3f, "Optimizing prompt...")
                val optimizerSystem = """
You are a prompt optimizer for PWA generation. Refine the user's prompt to be more detailed using this structure:

1. Core Functionality:
   - Primary Purpose: [Identify main app purpose]
   - Essential Features: [List core features]

2. UI/UX Enhancements:
   - Layout Suggestions: [Recommend layouts]
   - Animations/Transitions: [Suggest effects]
   - Dark/Light Mode: [Mandatory support]
   - Empty States: [Include placeholders]
   - Confirmation Dialogs: [For critical actions]

3. Responsiveness:
   - Tailwind CSS: [For adaptive design]
   - Mobile Interactions: [Touch-friendly elements]

4. Accessibility:
   - WCAG 2.1 AA Compliance: [Mandatory]
   - Keyboard Navigation: [Required]
   - ARIA Labels: [For interactive elements]
   - High Contrast: [For readability]

5. PWA Features:
   - Web App Manifest: [With icons/colors]
   - Service Worker: [For offline/cache]
   - Push Notifications: [For updates]

6. Data & Performance:
   - Storage: [Local/IndexedDB]
   - Lazy Loading: [For assets]
   - Optimization: [Fast load times]

7. Security & Extensibility:
   - Authentication: [If needed]
   - Backend: [Firebase/etc. options]

Output only the optimized prompt following this structure.
"""
                
                val optimizedPrompt = callOpenRouterApi(
                    apiKey = apiKey,
                    model = "mistralai/mistral-small-3.2-24b-instruct:free",
                    systemPrompt = optimizerSystem,
                    userContent = prompt
                )

                // Step 2: Generate Code
                updateProgress(0.6f, "Generating code...")
                val generatorSystem = """
ONLY USE HTML, CSS AND JAVASCRIPT. Create the best, modern, responsive UI possible.
MAKE IT RESPONSIVE USING TAILWINDCSS. Import Tailwind via CDN in <head> using <script src="https://cdn.tailwindcss.com"></script>.
Prefer Tailwind utility classes heavily for layout and components (CSS Grid for dashboards/cards, Flexbox for toolbars/forms). If Tailwind cannot cover a case, add minimal custom CSS in styles.css.
Use semantic HTML5 (<main>, <header>, <nav>, <section>, <footer>), accessible patterns (ARIA where needed), and a dark theme by default with good contrast.
If you want to use ICONS, import the icon library first. Use Feather Icons (https://unpkg.com/feather-icons) and/or Font Awesome (CDN) wherever icons help. If using Feather, add data-feather attributes and call feather.replace() in script.js after DOM load. If using Font Awesome, use <i> with appropriate classes.
Avoid Chinese characters unless explicitly requested by the user. Be creative and elaborate to produce something unique and polished.
IMPORTANT: For this API, ALWAYS output exactly three files: index.html (with Tailwind CDN + any icon CDN), styles.css (only minimal custom CSS), and script.js (modern JS, no frameworks). Do NOT inline CSS (except the Tailwind CDN include) and prefer Tailwind classes in markup.
Ensure offline compatibility with an external service worker (do not generate it inside these files). Include <link rel="manifest" href="manifest.json"> in index.html and service worker registration in script.js: if ('serviceWorker' in navigator) { navigator.serviceWorker.register('/sw.js'); }
Separate each file with the following exact tags: [HTML]...[/HTML] [CSS]...[/CSS] [JS]...[/JS]
"""
                
                val generatedCode = callOpenRouterApi(
                    apiKey = apiKey,
                    model = "qwen/qwen-2.5-coder-32b-instruct:free",
                    systemPrompt = generatorSystem,
                    userContent = "Create a PWA for: $optimizedPrompt"
                )

                // Step 3: Save files locally
                updateProgress(0.8f, "Saving files...")
                val appId = saveGeneratedFiles(prompt, generatedCode)

                // Complete generation
                updateProgress(1.0f, "PWA generation complete!")
                _isGenerating.value = false
                _progress.value = 0f
                
                // Add completion message
                val completionMessage = ChatMessage(
                    id = System.currentTimeMillis() + 2,
                    text = "Your PWA has been generated successfully! Files saved to: /generated/$appId/",
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + completionMessage
                
            } catch (e: Exception) {
                // Handle error
                _isGenerating.value = false
                _progress.value = 0f
                
                val errorMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    text = getErrorMessage(e),
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + errorMessage
            }
        }
    }

    private suspend fun callOpenRouterApi(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String
    ): String {
        val request = OpenRouterChatRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userContent)
            )
        )
        
        val response = api.chat(
            authorization = "Bearer $apiKey",
            referer = "https://mothership.app",
            title = "Mothership App",
            request = request
        )
        
        return response.choices.firstOrNull()?.message?.content ?: throw Exception("No response from API")
    }

    private fun saveGeneratedFiles(prompt: String, generatedCode: String): String {
        // Create a unique app ID
        val appId = UUID.randomUUID().toString()
        
        // Create the app directory in the Mothership folder
        val mothershipDir = File(context.getExternalFilesDir(null)?.parentFile, "Mothership/PWAs")
        val appDir = File(mothershipDir, appId)
        appDir.mkdirs()
        
        // Extract files from the generated code
        val files = extractFiles(generatedCode)
        
        // Save each file
        files.forEach { (filename, content) ->
            val file = File(appDir, filename)
            file.writeText(content)
        }
        
        // Create manifest.json
        val manifestContent = """
            {
              "name": "$prompt",
              "short_name": "${prompt.take(12)}",
              "start_url": "index.html",
              "display": "standalone",
              "background_color": "#ffffff",
              "theme_color": "#007BFF",
              "icons": [
                {
                  "src": "icon-192.png",
                  "sizes": "192x192",
                  "type": "image/png"
                },
                {
                  "src": "icon-512.png",
                  "sizes": "512x512",
                  "type": "image/png"
                }
              ]
            }
        """.trimIndent()
        val manifestFile = File(appDir, "manifest.json")
        manifestFile.writeText(manifestContent)
        
        // Create service worker
        val swContent = """
            self.addEventListener('install', e => {
              e.waitUntil(
                caches.open('pwa-store').then(cache => {
                  return cache.addAll([
                    './',
                    './index.html',
                    './styles.css',
                    './script.js',
                    './manifest.json'
                  ]);
                })
              );
            });

            self.addEventListener('fetch', e => {
              e.respondWith(
                caches.match(e.request).then(response => {
                  return response || fetch(e.request);
                })
              );
            });
        """.trimIndent()
        val swFile = File(appDir, "sw.js")
        swFile.writeText(swContent)
        
        // Create a simple info file for the app list
        val infoContent = """
            {
              "id": "$appId",
              "name": "$prompt",
              "created": "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}",
              "path": "${appDir.absolutePath}"
            }
        """.trimIndent()
        val infoFile = File(appDir, "app_info.json")
        infoFile.writeText(infoContent)
        
        return appId
    }

    private fun extractFiles(generatedCode: String): Map<String, String> {
        val files = mutableMapOf<String, String>()
        
        // Extract HTML
        val htmlPattern = Pattern.compile("\\[HTML\\](.*?)\\[/HTML\\]", Pattern.DOTALL)
        val htmlMatcher = htmlPattern.matcher(generatedCode)
        if (htmlMatcher.find()) {
            files["index.html"] = htmlMatcher.group(1).trim()
        }
        
        // Extract CSS
        val cssPattern = Pattern.compile("\\[CSS\\](.*?)\\[/CSS\\]", Pattern.DOTALL)
        val cssMatcher = cssPattern.matcher(generatedCode)
        if (cssMatcher.find()) {
            files["styles.css"] = cssMatcher.group(1).trim()
        }
        
        // Extract JS
        val jsPattern = Pattern.compile("\\[JS\\](.*?)\\[/JS\\]", Pattern.DOTALL)
        val jsMatcher = jsPattern.matcher(generatedCode)
        if (jsMatcher.find()) {
            files["script.js"] = jsMatcher.group(1).trim()
        }
        
        return files
    }

    private fun getErrorMessage(e: Exception): String {
        return when (e) {
            is SocketTimeoutException -> "Connection timeout. Please check your network connection and try again."
            is ConnectException -> "Failed to connect to the OpenRouter API. Please check your network connection and try again."
            is UnknownHostException -> "Unknown host. Please check your network connection and try again."
            else -> "Error: ${e.message ?: "Failed to generate PWA"}"
        }
    }

    private suspend fun updateProgress(progress: Float, message: String) {
        _progress.value = progress
        val progressMessage = ChatMessage(
            id = System.currentTimeMillis(),
            text = message,
            isUser = false
        )
        _chatMessages.value = _chatMessages.value + progressMessage
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)