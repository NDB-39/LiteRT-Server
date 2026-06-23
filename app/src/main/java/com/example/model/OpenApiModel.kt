package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "ok"
)

@Serializable
data class ModelsResponse(
    val models: List<ModelObj>
)

@Serializable
data class ModelObj(
    val id: String
)

@Serializable
data class LoadModelRequest(
    val path: String
)

@Serializable
data class SuccessResponse(
    val success: Boolean
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.8f,
    val max_tokens: Int = 512,
    val stream: Boolean = false,
    val conversation_id: String? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String?
)

@Serializable
data class ChatCompletionChunk(
    val id: String,
    val `object`: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<ChunkChoice>
)

@Serializable
data class ChunkChoice(
    val index: Int,
    val delta: ChunkDelta,
    val finish_reason: String?
)

@Serializable
data class ChunkDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class MetricsResponse(
    val ram_used_mb: Long,
    val tokens_per_second: Float,
    val backend: String,
    val model: String
)
