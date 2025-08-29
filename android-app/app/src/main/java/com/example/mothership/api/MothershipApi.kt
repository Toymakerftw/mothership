package com.example.mothership.api

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MothershipApi {

    @POST("/api/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse

    companion object {
        private const val BASE_URL = "https://openrouter.ai"

        fun create(): MothershipApi {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original.newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(MothershipApi::class.java)
        }
    }
}

// OpenRouter API models
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class OpenRouterChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageResponse
)

data class MessageResponse(
    val content: String
)

// App models (for local storage)
data class AppInfo(
    val name: String,
    val prompt: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("view_count") val viewCount: Int,
    @SerializedName("generate_count") val generateCount: Int,
    @SerializedName("has_index") val hasIndex: Boolean,
    @SerializedName("preview_url") val previewUrl: String?
)

data class AppListResponse(
    val apps: List<AppInfo>
)
