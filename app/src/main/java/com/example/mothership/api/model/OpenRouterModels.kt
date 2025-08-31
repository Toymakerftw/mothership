
package com.example.mothership.api.model

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
