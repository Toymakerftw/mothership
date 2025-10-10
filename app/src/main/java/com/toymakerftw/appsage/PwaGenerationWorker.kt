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
    }

    override suspend fun doWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT)
        val apiKey = inputData.getString(KEY_API_KEY)
        val selectedModelId = inputData.getString(KEY_SELECTED_MODEL) ?: "x-ai/grok-4-fast"

        if (prompt.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Prompt or API key is missing."))
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
                JSONObject(responseContent.trim())
            } catch (jsonException: Exception) {
                Log.w("PwaGenerationWorker", "Could not parse response as JSON, trying code blocks", jsonException)
                return extractFromCodeBlocks(responseContent, pwaDir, uuid)
            }

            val htmlContent = jsonResponse.optString("index.html", "")
            val cssContent = jsonResponse.optString("style.css", "")
            val jsContent = jsonResponse.optString("script.js", "")
            val manifestContent = jsonResponse.optString("manifest.json", "")

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

    private fun extractFromCodeBlocks(responseContent: String, pwaDir: File, uuid: String): String {
        File(pwaDir, "raw_response.txt").writeText(responseContent)

        val htmlMatch = Regex("```html\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val cssMatch = Regex("```css\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val jsMatch = Regex("```javascript\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
            ?: Regex("```js\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)
        val manifestMatch = Regex("```json\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL).find(responseContent)

        htmlMatch?.let { File(pwaDir, "index.html").writeText(it.groupValues[1]) }
        cssMatch?.let { File(pwaDir, "style.css").writeText(it.groupValues[1]) }
        jsMatch?.let { File(pwaDir, "script.js").writeText(it.groupValues[1]) }

        val finalManifestContent = manifestMatch?.groupValues?.get(1) ?: createDefaultManifest()
        File(pwaDir, "manifest.json").writeText(finalManifestContent)
        
        val pwaName = extractNameFromManifest(finalManifestContent)
        File(pwaDir, "app_info.json").writeText("{ \"name\": \"$pwaName\", \"uuid\": \"$uuid\" }")

        return uuid
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