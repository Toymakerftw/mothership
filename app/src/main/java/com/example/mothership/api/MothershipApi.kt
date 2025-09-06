
package com.example.mothership.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MothershipApi {

    @POST("chat/completions")
    suspend fun generatePwa(@Header("Authorization") apiKey: String, @Body request: com.example.mothership.api.model.OpenRouterRequest): com.example.mothership.api.model.OpenRouterResponse

    @POST("chat/completions")
    suspend fun rewritePrompt(@Header("Authorization") apiKey: String, @Body request: com.example.mothership.api.model.PromptRewriteRequest): com.example.mothership.api.model.PromptRewriteResponse
}
