
package com.toymakerftw.mothership.api.model

import com.google.gson.annotations.SerializedName

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

// Prompt rewriting specific models
data class PromptRewriteRequest(
    @SerializedName("model")
    val model: String = "mistralai/mistral-small-3.2-24b-instruct:free",
    @SerializedName("messages")
    val messages: List<Message>
)

data class PromptRewriteResponse(
    @SerializedName("choices")
    val choices: List<Choice>
)
