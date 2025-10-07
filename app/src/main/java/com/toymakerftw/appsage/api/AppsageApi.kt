package com.toymakerftw.appsage.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AppsageApi {

    @POST("chat/completions")
    suspend fun generatePwa(@Header("Authorization") apiKey: String, @Body request: OpenRouterRequest): OpenRouterResponse


}

data class OpenRouterRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<Message>
)

data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

data class OpenRouterResponse(
    @SerializedName("choices")
    val choices: List<Choice>
)

data class Choice(
    @SerializedName("message")
    val message: Message
)

