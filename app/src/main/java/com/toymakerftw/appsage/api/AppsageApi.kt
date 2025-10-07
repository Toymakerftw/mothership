package com.toymakerftw.appsage.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AppsageApi {

    @POST("chat/completions")
    suspend fun generatePwa(@Header("Authorization") apiKey: String, @Body request: OpenRouterRequest): OpenRouterResponse

    @GET("models")
    suspend fun getModels(@Header("Authorization") apiKey: String): OpenRouterModelsResponse
}

data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class OpenRouterResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class OpenRouterModelsResponse(
    val data: List<OpenRouterModelData>
)

data class OpenRouterModelData(
    val id: String,
    val name: String,
    val description: String?
)