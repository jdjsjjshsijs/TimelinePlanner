package com.example.timelineplanner.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface DeepSeekApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekChatMessage>,
    val temperature: Double = 0.1
)

data class DeepSeekChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val error: ApiError? = null
)

data class Choice(
    val index: Int? = null,
    val message: AssistantMessage? = null
)

data class AssistantMessage(
    val role: String? = null,
    val content: String? = null
)

data class ApiError(
    val message: String? = null,
    val type: String? = null
)
