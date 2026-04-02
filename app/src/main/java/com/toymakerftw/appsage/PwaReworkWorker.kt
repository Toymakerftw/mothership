package com.toymakerftw.appsage

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.toymakerftw.appsage.api.AppsageApi
import com.toymakerftw.appsage.api.Message
import com.toymakerftw.appsage.versioncontrol.VersionControl
import kotlinx.coroutines.delay
import java.io.File
import org.json.JSONObject

class PwaReworkWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val appsageApi: AppsageApi = AppsageApi.create()
    private val versionControl = VersionControl(context)

    companion object {
        const val KEY_UUID = "UUID"
        const val KEY_REWORK_PROMPT = "REWORK_PROMPT"
        const val KEY_API_KEY = "API_KEY"
        const val KEY_ERROR_MESSAGE = "ERROR_MESSAGE"
        const val KEY_GENERATION_STEP = "GENERATION_STEP"
    }

    override suspend fun doWork(): Result {
        val uuid = inputData.getString(KEY_UUID)
        val reworkPrompt = inputData.getString(KEY_REWORK_PROMPT)
        val apiKey = inputData.getString(KEY_API_KEY)

        if (uuid.isNullOrEmpty() || reworkPrompt.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "UUID, rework prompt, or API key is missing."))
        }

        try {
            // Step 0: Create backup
            setProgress(workDataOf(KEY_GENERATION_STEP to 0))
            delay(500)
            versionControl.createBackup(uuid, "Before rework: $reworkPrompt")

            // Step 1: Analyzing prompt
            setProgress(workDataOf(KEY_GENERATION_STEP to 1))
            delay(500)

            val pwaDir = File(context.getExternalFilesDir(null), uuid)
            if (!pwaDir.exists() || !pwaDir.isDirectory) {
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "PWA directory not found"))
            }

            // Step 2: Reading existing files
            setProgress(workDataOf(KEY_GENERATION_STEP to 2))
            delay(500)

            val filesToRead = listOf("index.html", "style.css", "script.js", "manifest.json")
            val fileContents = mutableMapOf<String, String>()
            filesToRead.forEach { fileName ->
                val file = File(pwaDir, fileName)
                if (file.exists()) {
                    fileContents[fileName] = file.readText()
                }
            }

            if (fileContents.isEmpty()) {
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "No files to rework"))
            }

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

            // Step 4: Generating code
            setProgress(workDataOf(KEY_GENERATION_STEP to 4))
            delay(500)

            val selectedModelId = "gemini-2.5-flash"
            val fullPromptWithRequirements = fullPrompt + """

CRITICAL VIBE SDK STANDARDS FOR THE OUTPUT:
1. Polished UI/UX: Use modern CSS (Flexbox, Grid, Variables), smooth transitions, and high-quality aesthetics.
2. Mobile-First & Responsive: Viewport meta tag is MANDATORY. Design for touch first, then adapt for desktop.
3. JS TECHNICAL REQUIREMENTS (CRITICAL):
   - Wrap all logic in `document.addEventListener('DOMContentLoaded', ...)` to prevent "null element" errors.
   - Use `const` and `let` only. No `var`.
   - Ensure script.js is correctly linked at the end of index.html's `<body>`.
   - Ensure all updated or new features have robust JS logic with error handling.
4. Clean, Modular Code: Write well-structured HTML and JavaScript (ES6+).
5. Interactive Feedback: Ensure the UI responds to user input with animations or state changes.
6. Accessibility: Use semantic tags and proper ARIA labels.
7. Installable: Provide a comprehensive manifest.json with appropriate icons and theme colors.

OUTPUT FORMAT (JSON ONLY):
{
  "index.html": "<!DOCTYPE html>...",
  "style.css": ":root { ... }",
  "script.js": "document.addEventListener('...', () => { ... });",
  "manifest.json": "{\"name\": \"...\", \"short_name\": \"...\", \"theme_color\": \"...\", \"background_color\": \"...\", \"display\": \"standalone\", \"start_url\": \"/index.html\"}"
}

IMPORTANT: Return ONLY the JSON object with the updated files. No markdown, no explanations, no text outside the JSON structure."""

            val response = appsageApi.generatePwa(apiKey, selectedModelId, listOf(Message("user", fullPromptWithRequirements)))

            if (response != null) {
                updatePwaCode(uuid, response)
                versionControl.clearOldVersions(uuid)
                return Result.success()
            } else {
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "API returned no response."))
            }
        } catch (e: Exception) {
            Log.e("PwaReworkWorker", "Error reworking PWA", e)
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to e.message))
        }
    }

    private fun updatePwaCode(uuid: String, responseContent: String) {
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        if (!pwaDir.exists()) return

        try {
            val jsonResponse = JSONObject(responseContent.trim())
            val keys = jsonResponse.keys()
            var updated = false
            for (key in keys) {
                if (jsonResponse.get(key) is String) {
                    var content = jsonResponse.getString(key)
                    if (key == "index.html") {
                        content = ensureEssentialTags(content)
                    }
                    File(pwaDir, key).writeText(content)
                    updated = true
                }
            }
            if (!updated) {
                extractFromCodeBlocks(uuid, responseContent)
            }
        } catch (e: Exception) {
            extractFromCodeBlocks(uuid, responseContent)
        }
    }

    private fun extractFromCodeBlocks(uuid: String, responseContent: String) {
        val pwaDir = File(context.getExternalFilesDir(null), uuid)
        if (!pwaDir.exists()) return

        val htmlMatch = Regex("```html\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val cssMatch = Regex("```css\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val jsMatch = Regex("```javascript\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
            ?: Regex("```js\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val jsonMatch = Regex("```json\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)

        htmlMatch?.let { File(pwaDir, "index.html").writeText(ensureEssentialTags(it.groupValues[1])) }
        cssMatch?.let { File(pwaDir, "style.css").writeText(it.groupValues[1]) }
        jsMatch?.let { File(pwaDir, "script.js").writeText(it.groupValues[1]) }
        jsonMatch?.let { File(pwaDir, "manifest.json").writeText(it.groupValues[1]) }
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

        // Ensure viewport meta tag exists
        if (!processedHtml.contains("name=\"viewport\"", ignoreCase = true)) {
            processedHtml = processedHtml.replaceFirst(Regex("<head[^>]*>", RegexOption.IGNORE_CASE), "$0\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
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