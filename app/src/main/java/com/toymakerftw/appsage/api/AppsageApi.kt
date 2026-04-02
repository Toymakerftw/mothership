package com.toymakerftw.appsage.api

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

class AppsageApi {

    suspend fun generatePwa(apiKey: String, modelId: String, messages: List<Message>): String? {
        val generativeModel = GenerativeModel(
            modelName = modelId,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            systemInstruction = content {
                text("""
                    You are an expert PWA developer following the Cloudflare Vibe SDK standards. 
                    Your goal is to generate high-quality, modern, and visually appealing Progressive Web Apps.
                    
                    PRINCIPLES:
                    1. Polished UI/UX: Use modern design patterns, interactive elements, and interactive feedback.
                    2. Mobile-First: Prioritize mobile responsiveness and touch-friendly interactions.
                    3. Clean Code: Write modular, well-commented HTML, CSS (using variables), and JavaScript.
                    4. Robust JavaScript:
                       - ALWAYS wrap code in `document.addEventListener('DOMContentLoaded', ...)` or use modules.
                       - Use `const` and `let`, never `var`.
                       - Implement proper error handling with try-catch blocks, especially for `fetch` and `localStorage`.
                       - Ensure all event listeners are correctly attached to elements that exist.
                       - Use descriptive variable and function names.
                    5. Performance: Ensure fast loading times and efficient code.
                    6. Accessibility: Use semantic HTML and ARIA roles for inclusivity.
                    7. PWA Features: Include a complete manifest.json and ensure the app is installable.
                    
                    STYLING: Use modern Vanilla CSS with Flexbox/Grid and CSS variables. If requested, you can use Tailwind CSS via CDN.
                    ARCHITECTURE: Keep logic separate from presentation. Use modern ES6+ features in JavaScript.
                """.trimIndent())
            }
        )

        // Convert messages to content for the SDK
        // In this case, we'll focus on the user's latest prompt
        // Or if multiple messages are needed, we can construct them.
        // For PWA generation, it's usually one big prompt.
        val chat = generativeModel.startChat()
        
        // If there's only one message (the prompt), we can use generateContent
        // If there are multiple, we could potentially feed them to the chat.
        // Let's assume the last message is the one we want to send.
        val lastMessage = messages.lastOrNull()?.content ?: return null
        
        return try {
            val response = generativeModel.generateContent(lastMessage)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchAvailableModels(apiKey: String): List<String> {
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = org.json.JSONObject(body)
                val modelsArray = json.getJSONArray("models")
                val modelList = mutableListOf<String>()
                for (i in 0 until modelsArray.length()) {
                    val model = modelsArray.getJSONObject(i)
                    val name = model.getString("name").removePrefix("models/")
                    // Only include models that support content generation
                    val supportedMethods = model.getJSONArray("supportedGenerationMethods")
                    for (j in 0 until supportedMethods.length()) {
                        if (supportedMethods.getString(j) == "generateContent") {
                            modelList.add(name)
                            break
                        }
                    }
                }
                modelList.sorted()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    companion object {
        fun create(): AppsageApi {
            return AppsageApi()
        }
    }
}

data class Message(
    val role: String,
    val content: String
)
