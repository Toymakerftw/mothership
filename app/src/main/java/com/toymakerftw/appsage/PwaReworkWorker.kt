package com.toymakerftw.appsage

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.toymakerftw.appsage.api.AppsageApi
import com.toymakerftw.appsage.api.Message
import com.toymakerftw.appsage.api.OpenRouterRequest
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

            // Determine if reasoning should be disabled for specific models
            val selectedModelId = "x-ai/grok-4-fast" // Currently hardcoded, but can be changed in future
            val disableReasoning = selectedModelId in listOf(
                "tngtech/deepseek-r1t2-chimera:free",
                "openai/gpt-oss-20b:free"
            )
            
            val temperature = if (disableReasoning) 0.1f else 0.7f // Lower temperature for more deterministic responses when reasoning is disabled
            
            val request = OpenRouterRequest(
                model = selectedModelId,
                messages = listOf(Message(role = "user", content = fullPrompt + """

IMPORTANT: Return only the JSON object with the updated files. Do not include any explanation, reasoning, or additional text before or after the JSON. The response should begin and end with the JSON structure. Do not wrap the JSON in markdown code blocks if possible.""")),
                temperature = temperature,
                stream = false
            )

            val response = appsageApi.generatePwa("Bearer $apiKey", request)

            if (response.choices.isNotEmpty()) {
                val content = response.choices[0].message.content
                updatePwaCode(uuid, content)
                versionControl.clearOldVersions(uuid)
                return Result.success()
            } else {
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "API returned no choices."))
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
                    val content = jsonResponse.getString(key)
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

        htmlMatch?.let { File(pwaDir, "index.html").writeText(it.groupValues[1]) }
        cssMatch?.let { File(pwaDir, "style.css").writeText(it.groupValues[1]) }
        jsMatch?.let { File(pwaDir, "script.js").writeText(it.groupValues[1]) }
        jsonMatch?.let { File(pwaDir, "manifest.json").writeText(it.groupValues[1]) }
    }
}