package com.example.mothership.service

import android.util.Log
import com.example.mothership.api.MothershipApi
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.PromptRewriteRequest
import com.example.mothership.service.JsonPromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PromptRewriteService(private val mothershipApi: MothershipApi) {

    companion object {
        private const val TAG = "PromptRewriteService"
        
        // Rewriting prompt as specified in the documentation
        private val REWRITING_PROMPT = JsonPromptBuilder().buildPromptRewritePrompt()
    }

    /**
     * Rewrites a prompt using the mistralai model to make it more detailed and specific
     * @param originalPrompt The original user prompt
     * @param apiKey The API key to use for the request
     * @return The rewritten prompt, or the original prompt if rewriting fails
     */
    suspend fun rewritePrompt(originalPrompt: String, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting prompt rewrite for: ${originalPrompt.take(100)}...")
                Log.d(TAG, "API Key length: ${apiKey.length}")
                
                val request = PromptRewriteRequest(
                    messages = listOf(
                        Message(
                            role = "system",
                            content = REWRITING_PROMPT
                        ),
                        Message(
                            role = "user",
                            content = originalPrompt
                        )
                    )
                )

                Log.d(TAG, "Making API request to rewrite prompt...")
                val response = mothershipApi.rewritePrompt("Bearer $apiKey", request)
                Log.d(TAG, "Received response from prompt rewriting API")
                
                if (response.choices.isEmpty()) {
                    Log.w(TAG, "No response from prompt rewriting API")
                    return@withContext originalPrompt
                }

                val rewrittenContent = response.choices.first().message.content
                val extractedPrompt = extractRewrittenPrompt(rewrittenContent, originalPrompt)
                
                Log.d(TAG, "Prompt rewrite completed. Original length: ${originalPrompt.length}, Rewritten length: ${extractedPrompt.length}")
                extractedPrompt
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during prompt rewriting: ${e.javaClass.simpleName}", e)
                Log.e(TAG, "Error message: ${e.message}")
                // Return original prompt if rewriting fails
                originalPrompt
            }
        }
    }

    /**
     * Extracts the rewritten prompt from the AI response using the specified markers
     * @param response The AI response containing the rewritten prompt
     * @param originalPrompt The original prompt as fallback
     * @return The extracted rewritten prompt or the original prompt if extraction fails
     */
    private fun extractRewrittenPrompt(response: String, originalPrompt: String): String {
        return try {
            val startMarker = ">>>>>>> START PROMPT >>>>>>"
            val endMarker = ">>>>>>> END PROMPT >>>>>>"
            val codeBlockMarker = "```"

            // Prioritize markers for extraction
            val markerStartIndex = response.indexOf(startMarker)
            val markerEndIndex = response.indexOf(endMarker)

            if (markerStartIndex != -1 && markerEndIndex != -1 && markerEndIndex > markerStartIndex) {
                val extractedPrompt = response.substring(
                    markerStartIndex + startMarker.length,
                    markerEndIndex
                ).trim()
                if (extractedPrompt.isNotEmpty()) {
                    Log.d(TAG, "Successfully extracted rewritten prompt using markers")
                    return extractedPrompt
                }
            }

            // Fallback to code blocks if markers are not found or extraction fails
            val codeBlockStartIndex = response.indexOf(codeBlockMarker)
            if (codeBlockStartIndex != -1) {
                val codeBlockEndIndex = response.indexOf(
                    codeBlockMarker,
                    startIndex = codeBlockStartIndex + codeBlockMarker.length
                )
                if (codeBlockEndIndex != -1) {
                    val extractedPrompt = response.substring(
                        codeBlockStartIndex + codeBlockMarker.length,
                        codeBlockEndIndex
                    ).trim()
                    if (extractedPrompt.isNotEmpty()) {
                        Log.d(TAG, "Successfully extracted rewritten prompt using code blocks")
                        return extractedPrompt
                    }
                }
            }

            // If all extraction methods fail, return the original prompt
            Log.w(TAG, "Could not find markers or code blocks in response, using original")
            originalPrompt
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting rewritten prompt", e)
            originalPrompt
        }
    }
}
