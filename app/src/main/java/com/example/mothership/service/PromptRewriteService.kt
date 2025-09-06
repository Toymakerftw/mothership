package com.example.mothership.service

import android.util.Log
import com.example.mothership.api.MothershipApi
import com.example.mothership.api.model.Message
import com.example.mothership.api.model.PromptRewriteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PromptRewriteService(private val mothershipApi: MothershipApi) {

    companion object {
        private const val TAG = "PromptRewriteService"
        
        // Rewriting prompt as specified in the documentation
        private val REWRITING_PROMPT = """
            You are a helpful assistant that rewrites prompts to make them better. All the prompts will be about creating a website or app.  
            Try to make the prompt more detailed and specific to create a good UI/UX Design and good code.  
            Format the result by following this format:  
            >>>>>>> START PROMPT >>>>>>  
            new prompt here  
            >>>>>>> END PROMPT >>>>>>  
            If you don't rewrite the prompt, return the original prompt.  
            Make sure to return the prompt in the same language as the prompt you are given. Also IMPORTANT: Make sure to keep the original intent of the prompt. Improve it it needed, but don't change the    
            original intent.
        """.trimIndent()
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
            
            val startIndex = response.indexOf(startMarker)
            val endIndex = response.indexOf(endMarker)
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                val extractedPrompt = response.substring(
                    startIndex + startMarker.length,
                    endIndex
                ).trim()
                
                if (extractedPrompt.isNotEmpty()) {
                    Log.d(TAG, "Successfully extracted rewritten prompt")
                    extractedPrompt
                } else {
                    Log.w(TAG, "Extracted prompt is empty, using original")
                    originalPrompt
                }
            } else {
                Log.w(TAG, "Could not find prompt markers in response, using original")
                originalPrompt
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting rewritten prompt", e)
            originalPrompt
        }
    }
}
