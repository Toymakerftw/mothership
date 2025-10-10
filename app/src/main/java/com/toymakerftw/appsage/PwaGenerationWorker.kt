package com.toymakerftw.appsage

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.toymakerftw.appsage.api.AppsageApi
import com.toymakerftw.appsage.api.Message
import com.toymakerftw.appsage.api.OpenRouterRequest
import com.toymakerftw.appsage.data.SettingsRepository
import kotlinx.coroutines.delay
import java.io.File
import java.io.EOFException
import java.util.UUID
import org.json.JSONObject

class PwaGenerationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val appsageApi: AppsageApi = AppsageApi.create()

    companion object {
        const val KEY_PROMPT = "PROMPT"
        const val KEY_API_KEY = "API_KEY"
        const val KEY_SELECTED_MODEL = "SELECTED_MODEL"
        const val KEY_PWA_UUID = "PWA_UUID"
        const val KEY_ERROR_MESSAGE = "ERROR_MESSAGE"
        const val KEY_GENERATION_STEP = "GENERATION_STEP"
        const val MAX_PROMPT_LENGTH = 2000 // Maximum allowed prompt length
    }

    override suspend fun doWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT)
        val apiKey = inputData.getString(KEY_API_KEY)
        val selectedModelId = inputData.getString(KEY_SELECTED_MODEL) ?: "x-ai/grok-4-fast"

        if (prompt.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Prompt or API key is missing."))
        }
        
        // Validate prompt length
        if (prompt.length > MAX_PROMPT_LENGTH) {
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Prompt exceeds maximum allowed length of ${MAX_PROMPT_LENGTH} characters."))
        }

        try {
            // Step 0: Analyzing prompt
            setProgress(workDataOf(KEY_GENERATION_STEP to 0))
            delay(500)

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
  "manifest.json": "{\"name\": \"My PWA App\", \"short_name\": \"PWA App\"}"
}"""
                    )
                )
            )

            // Step 1: Generating code
            setProgress(workDataOf(KEY_GENERATION_STEP to 1))
            val response = appsageApi.generatePwa("Bearer $apiKey", request)

            // Step 2: Styling UI
            setProgress(workDataOf(KEY_GENERATION_STEP to 2))
            delay(300)

            if (response.choices.isNotEmpty()) {
                val content = response.choices[0].message.content
                
                // Step 3: Finalizing
                setProgress(workDataOf(KEY_GENERATION_STEP to 3))
                
                val pwaUuid = extractAndSavePwaCode(content)
                return Result.success(workDataOf(KEY_PWA_UUID to pwaUuid))
            } else {
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "API returned no choices."))
            }
        } catch (e: Exception) {
            Log.e("PwaGenerationWorker", "Error generating PWA", e)
            val errorMessage = if (e is EOFException || e.cause is EOFException) {
                "API response was incomplete. This may be due to a network timeout or connection issue. Please try again."
            } else {
                e.message ?: "An unknown error occurred."
            }
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to errorMessage))
        }
    }

    private fun extractAndSavePwaCode(responseContent: String): String {
        val uuid = UUID.randomUUID().toString()
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        pwaDir.mkdirs()

        try {
            val jsonResponse = try {
                // Validate and sanitize the JSON before using it
                if (!isValidJsonStructure(responseContent.trim())) {
                    Log.w("PwaGenerationWorker", "Invalid JSON structure received, trying code blocks")
                    return extractFromCodeBlocks(responseContent, pwaDir, uuid)
                }
                JSONObject(responseContent.trim())
            } catch (jsonException: Exception) {
                Log.w("PwaGenerationWorker", "Could not parse response as JSON, trying code blocks", jsonException)
                return extractFromCodeBlocks(responseContent, pwaDir, uuid)
            }

            // Sanitize the content to prevent potential XSS or other injection issues
            val htmlContent = sanitizeContent(jsonResponse.optString("index.html", ""))
            val cssContent = sanitizeContent(jsonResponse.optString("style.css", ""))
            val jsContent = sanitizeContent(jsonResponse.optString("script.js", ""))
            val manifestContent = sanitizeManifestContent(jsonResponse.optString("manifest.json", ""))

            if (htmlContent.isEmpty() && cssContent.isEmpty() && jsContent.isEmpty()) {
                return extractFromCodeBlocks(responseContent, pwaDir, uuid)
            } else {
                if (htmlContent.isNotEmpty()) File(pwaDir, "index.html").writeText(htmlContent)
                if (cssContent.isNotEmpty()) File(pwaDir, "style.css").writeText(cssContent)
                if (jsContent.isNotEmpty()) File(pwaDir, "script.js").writeText(jsContent)

                val finalManifestContent = if (manifestContent.isNotEmpty()) manifestContent else createDefaultManifest()
                File(pwaDir, "manifest.json").writeText(finalManifestContent)

                val pwaName = extractNameFromManifest(finalManifestContent)
                File(pwaDir, "app_info.json").writeText("{ \"name\": \"$pwaName\", \"uuid\": \"$uuid\" }")
                
                return uuid
            }
        } catch (e: Exception) {
            Log.e("PwaGenerationWorker", "Error extracting PWA code", e)
            // In case of error, we still have the raw response saved in extractFromCodeBlocks
            throw e // Re-throw to be caught by the main try-catch and result in failure
        }
    }
    
    private fun isValidJsonStructure(jsonStr: String): Boolean {
        return try {
            val obj = JSONObject(jsonStr)
            // Basic validation - check if it has at least one of the expected keys
            obj.has("index.html") || obj.has("style.css") || obj.has("script.js") || obj.has("manifest.json")
        } catch (e: Exception) {
            false
        }
    }
    
    private fun sanitizeContent(content: String): String {
        // Basic sanitization to remove potentially harmful content
        // In a real application, you would want more thorough sanitization
        return content
            .replace(Regex("<script[\\s\\S]*?>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("javascript:", RegexOption.IGNORE_CASE), "js:")
            .replace(Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE), "sanitized_")
    }
    
    private fun sanitizeManifestContent(content: String): String {
        // For manifest content, ensure it's valid JSON
        if (content.isEmpty()) return content
        return try {
            // Attempt to parse and re-serialize to ensure valid structure
            val manifestObj = JSONObject(content)
            manifestObj.toString()
        } catch (e: Exception) {
            Log.w("PwaGenerationWorker", "Invalid manifest JSON, using default", e)
            createDefaultManifest()
        }
    }

    private fun extractFromCodeBlocks(responseContent: String, pwaDir: File, uuid: String): String {
        // Sanitize the raw response before saving it
        val sanitizedResponse = sanitizeRawResponse(responseContent)
        File(pwaDir, "raw_response.txt").writeText(sanitizedResponse)

        val htmlMatch = Regex("```html\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val cssMatch = Regex("```css\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val jsMatch = Regex("```javascript\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
            ?: Regex("```js\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val manifestMatch = Regex("```json\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)

        htmlMatch?.let { 
            val sanitizedHtml = sanitizeContent(it.groupValues[1]) 
            File(pwaDir, "index.html").writeText(sanitizedHtml) 
        }
        cssMatch?.let { 
            val sanitizedCss = sanitizeContent(it.groupValues[1]) 
            File(pwaDir, "style.css").writeText(sanitizedCss) 
        }
        jsMatch?.let { 
            val sanitizedJs = sanitizeContent(it.groupValues[1]) 
            File(pwaDir, "script.js").writeText(sanitizedJs) 
        }

        val finalManifestContent = manifestMatch?.groupValues?.get(1) ?: createDefaultManifest()
        val sanitizedManifest = sanitizeManifestContent(finalManifestContent)
        File(pwaDir, "manifest.json").writeText(sanitizedManifest)
        
        val pwaName = extractNameFromManifest(sanitizedManifest)
        File(pwaDir, "app_info.json").writeText("{ \"name\": \"$pwaName\", \"uuid\": \"$uuid\" }")

        return uuid
    }
    
    private fun sanitizeRawResponse(response: String): String {
        // Remove API keys, secrets, or other sensitive information from the raw response
        return response.replace(Regex("API_KEY|token|secret|password|auth|bearer", 
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)), "[REDACTED]")
    }

    private fun createDefaultManifest(): String = """{
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

    private fun extractNameFromManifest(manifestContent: String): String {
        return try {
            val manifestJson = JSONObject(manifestContent)
            val shortName = manifestJson.optString("short_name")
            val manifestName = manifestJson.optString("name")
            shortName.ifEmpty { manifestName }.ifEmpty { "Generated PWA" }
        } catch (e: Exception) {
            Log.w("PwaGenerationWorker", "Could not extract name from manifest", e)
            "Generated PWA"
        }
    }
}