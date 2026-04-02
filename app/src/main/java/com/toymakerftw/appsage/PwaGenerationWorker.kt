package com.toymakerftw.appsage

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.toymakerftw.appsage.api.AppsageApi
import com.toymakerftw.appsage.api.Message
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
        val selectedModelId = inputData.getString(KEY_SELECTED_MODEL) ?: "gemini-2.5-flash"

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

            val fullPrompt = """Generate a high-quality, modern, and visually appealing mobile-first PWA with HTML, CSS, and JavaScript code in JSON format, following the Cloudflare Vibe SDK style. 
The PWA should implement: $prompt.

Include index.html, style.css, script.js, and manifest.json in the JSON response.

CRITICAL VIBE SDK STANDARDS:
1. Polished UI/UX: Use modern CSS (Flexbox, Grid, Variables), smooth transitions, and high-quality aesthetics.
2. Mobile-First & Responsive: Viewport meta tag is MANDATORY. Design for touch first, then adapt for desktop.
3. JS TECHNICAL REQUIREMENTS (IMPORTANT):
   - Wrap all logic in `document.addEventListener('DOMContentLoaded', ...)` to prevent "null element" errors.
   - Use `const` and `let` only. No `var`.
   - Implement error handling for any API calls or data persistence.
   - Ensure script.js is correctly linked at the end of index.html's `<body>`.
   - IDs used in `document.getElementById` MUST match the IDs defined in the HTML exactly.
4. Clean, Modular Code: Write well-structured HTML and JavaScript (ES6+).
5. Interactive Feedback: Ensure the UI responds to user input with animations or state changes.
6. Accessibility: Use semantic tags and proper ARIA labels.
7. Installable: Provide a comprehensive manifest.json with appropriate icons and theme colors.
8. Adaptability & Scaling (CRITICAL): The layout MUST adapt seamlessly to different display sizes and pixel densities. Never use hardcoded pixel heights that cause vertical clipping. Use `dvh` or `svh` for viewport heights, flexbox with sensible shrinking, and fluid typography (e.g. `clamp()`). Incorporate `overflow-y: auto` for main scrollable areas. Use `env(safe-area-inset-top, 32px)` and `env(safe-area-inset-bottom, 32px)` for padding to prevent UI from hiding behind system bars.

OUTPUT FORMAT (JSON ONLY):
{
  "index.html": "<!DOCTYPE html>...",
  "style.css": ":root { ... }",
  "script.js": "document.addEventListener('...', () => { ... });",
  "manifest.json": "{\"name\": \"...\", \"short_name\": \"...\", \"theme_color\": \"...\", \"background_color\": \"...\", \"display\": \"standalone\", \"start_url\": \"/index.html\"}"
}

IMPORTANT: Return ONLY the JSON object. No markdown, no explanations, no text outside the JSON structure."""

            // Step 1: Generating code
            setProgress(workDataOf(KEY_GENERATION_STEP to 1))
            val response = appsageApi.generatePwa(apiKey, selectedModelId, listOf(Message("user", fullPrompt)))

            // Step 2: Styling UI
            setProgress(workDataOf(KEY_GENERATION_STEP to 2))
            delay(300)

            if (response != null) {
                // Step 3: Finalizing
                setProgress(workDataOf(KEY_GENERATION_STEP to 3))
                
                val pwaUuid = extractAndSavePwaCode(response)
                return Result.success(workDataOf(KEY_PWA_UUID to pwaUuid))
            } else {
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "API returned no response."))
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
            // First, try to extract JSON by looking for the actual JSON object in the response
            // This handles cases where reasoning or other text surrounds the JSON
            val extractedJson = extractJsonFromResponse(responseContent)
            
            val jsonResponse = try {
                // Validate and sanitize the JSON before using it
                if (!isValidJsonStructure(extractedJson.trim())) {
                    Log.w("PwaGenerationWorker", "Invalid JSON structure received, trying code blocks")
                    return extractFromCodeBlocks(responseContent, pwaDir, uuid)
                }
                JSONObject(extractedJson.trim())
            } catch (jsonException: Exception) {
                Log.w("PwaGenerationWorker", "Could not parse response as JSON, trying code blocks", jsonException)
                return extractFromCodeBlocks(responseContent, pwaDir, uuid)
            }

            // Sanitize the content to prevent potential XSS or other injection issues
            var htmlContent = sanitizeContent(jsonResponse.optString("index.html", ""))
            htmlContent = ensureEssentialTags(htmlContent)
            val cssContent = jsonResponse.optString("style.css", "")
            val jsContent = jsonResponse.optString("script.js", "")
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
    
    private fun extractJsonFromResponse(responseContent: String): String {
        var content = responseContent.trim()
        
        // Look for JSON object pattern - starting with { and ending with }
        val jsonStartIndex = content.indexOf('{')
        val jsonEndIndex = content.lastIndexOf('}')
        
        if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
            // Extract the main JSON object
            content = content.substring(jsonStartIndex, jsonEndIndex + 1)
        }
        
        // Remove any extra text that might be around the JSON
        // For example, if there are newlines or markdown formatting
        return content.trim()
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
            .replace(Regex("javascript:", RegexOption.IGNORE_CASE), "js:")
            .replace(Regex("\\bon\\w+\\s*=", RegexOption.IGNORE_CASE), "sanitized_")
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

        // Use a single pass to extract all code blocks more efficiently
        val allBlocksRegex = Regex("```(\\w+)\\s*\\n(.*?)\\s*```", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
        val allMatches = allBlocksRegex.findAll(responseContent).associate { match ->
            match.groupValues[1].lowercase() to match.groupValues[2]
        }

        // Extract specific file types
        val htmlContent = allMatches["html"]
        val cssContent = allMatches["css"]
        val jsContent = allMatches["javascript"] ?: allMatches["js"]
        val manifestContent = allMatches["json"]

        htmlContent?.let { 
            val sanitizedHtml = ensureEssentialTags(sanitizeContent(it)) 
            File(pwaDir, "index.html").writeText(sanitizedHtml) 
        }
        cssContent?.let { 
            File(pwaDir, "style.css").writeText(it) 
        }
        jsContent?.let { 
            val validatedJs = validateAndSanitizeJS(it)
            File(pwaDir, "script.js").writeText(validatedJs) 
        }

        val finalManifestContent = manifestContent ?: createDefaultManifest()
        val sanitizedManifest = sanitizeManifestContent(finalManifestContent)
        File(pwaDir, "manifest.json").writeText(sanitizedManifest)
        
        val pwaName = extractNameFromManifest(sanitizedManifest)
        File(pwaDir, "app_info.json").writeText("{ \"name\": \"$pwaName\", \"uuid\": \"$uuid\" }")

        return uuid
    }
    
    private fun sanitizeRawResponse(response: String): String {
        // Remove API keys, secrets, or other sensitive information from the raw response
        var sanitized = response
        // More comprehensive sanitization to prevent sensitive data leakage
        sanitized = sanitized.replace(
            Regex("(API_KEY|token|secret|password|auth|bearer|key|\\w*api\\w*|\\w*token\\w*)\\s*[:=]\\s*[\"']?\\w+[\"']?", 
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)), 
            "\$1: [SANITIZED]"
        )
        return sanitized
    }
    
    private fun validateAndSanitizeJS(content: String): String {
        // Check for potentially dangerous JavaScript patterns
        if (content.contains(Regex("eval\\s*\\(|document\\.cookie|localStorage|sessionStorage|openDatabase|indexedDB", 
                RegexOption.IGNORE_CASE))) {
            throw SecurityException("Potentially malicious content detected in JS code")
        }
        return content
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

    private fun ensureEssentialTags(html: String): String {
        if (html.isEmpty()) return html
        var processedHtml = html
        
        // Add basic HTML structure if missing
        if (!processedHtml.contains("<html", ignoreCase = true)) {
            processedHtml = "<!DOCTYPE html>\n<html>\n<head></head>\n<body>\n$processedHtml\n</body>\n</html>"
        }
        if (!processedHtml.contains("<head", ignoreCase = true)) {
            processedHtml = processedHtml.replaceFirst(Regex("<html[^>]*>", RegexOption.IGNORE_CASE), "$0\n<head></head>")
        }
        if (!processedHtml.contains("<body", ignoreCase = true)) {
            processedHtml = processedHtml.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "</head>\n<body>")
            processedHtml = processedHtml.replaceFirst(Regex("</html>", RegexOption.IGNORE_CASE), "</body>\n</html>")
        }

        // Ensure viewport meta tag exists and has viewport-fit=cover
        if (!processedHtml.contains("name=\"viewport\"", ignoreCase = true)) {
            processedHtml = processedHtml.replaceFirst(Regex("<head[^>]*>", RegexOption.IGNORE_CASE), "$0\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover\">")
        } else if (!processedHtml.contains("viewport-fit", ignoreCase = true)) {
            processedHtml = processedHtml.replace(Regex("(<meta[^>]*name=[\"']viewport[\"'][^>]*content=[\"'])([^\"']*)([\"'][^>]*>)", RegexOption.IGNORE_CASE)) { matchResult ->
                val content = matchResult.groupValues[2]
                val separator = if (content.trim().isNotEmpty() && !content.trim().endsWith(",")) ", " else ""
                "${matchResult.groupValues[1]}${content}${separator}viewport-fit=cover${matchResult.groupValues[3]}"
            }
        }

        // Ensure script.js is linked
        if (!processedHtml.contains("script.js", ignoreCase = true)) {
            processedHtml = processedHtml.replaceFirst(Regex("</body>", RegexOption.IGNORE_CASE), "    <script src=\"script.js\"></script>\n</body>")
        }

        // Ensure style.css is linked
        if (!processedHtml.contains("style.css", ignoreCase = true)) {
            processedHtml = processedHtml.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "    <link rel=\"stylesheet\" href=\"style.css\">\n</head>")
        }

        return processedHtml
    }
}
