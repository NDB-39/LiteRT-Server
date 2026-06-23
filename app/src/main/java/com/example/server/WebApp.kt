package com.example.server

import android.content.Context
import com.example.litert.EngineManager
import com.example.metrics.MetricsManager
import com.example.model.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

fun startServer(androidContext: Context): CIOApplicationEngine {
    val engine = embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        routing {
            get("/health") {
                call.respond(HealthResponse("ok"))
            }

            get("/models") {
                val modelId = if (EngineManager.instance.isLoaded)MetricsManager.currentModelName else "gemma-4-e2b"
                call.respond(ModelsResponse(listOf(ModelObj(modelId))))
            }

            post("/load_model") {
                val req = call.receive<LoadModelRequest>()
                try {
                    EngineManager.instance.loadModel(androidContext, req.path)
                    call.respond(SuccessResponse(true))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

            post("/unload_model") {
                try {
                    EngineManager.instance.unloadModel()
                    call.respond(SuccessResponse(true))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

            post("/v1/chat/completions") {
                val req = call.receive<ChatCompletionRequest>()
                
                val conversation = ContextManager.getOrCreateConversation(req.conversation_id)
                ContextManager.appendMessages(conversation, req.messages)

                val prompt = ContextManager.buildPrompt(conversation)
                val modelId = if (EngineManager.instance.isLoaded)MetricsManager.currentModelName else req.model

                if (req.stream) {
                    call.response.cacheControl(CacheControl.NoCache(null))
                    call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                        val stream = EngineManager.instance.streamTokens(prompt)
                        val id = "chatcmpl-\${System.currentTimeMillis()}"
                        var index = 0
                        
                        stream.collect { token ->
                            val chunk = ChatCompletionChunk(
                                id = id,
                                created = System.currentTimeMillis() / 1000,
                                model = modelId,
                                choices = listOf(
                                    ChunkChoice(
                                        index = 0,
                                        delta = ChunkDelta(if (index == 0) "assistant" else null, token),
                                        finish_reason = null
                                    )
                                )
                            )
                            val jsonString = Json.encodeToString(ChatCompletionChunk.serializer(), chunk)
                            writeStringUtf8("data: \$jsonString\\n\\n")
                            flush()
                            index++
                        }
                        
                        val finishChunk = ChatCompletionChunk(
                            id = id,
                            created = System.currentTimeMillis() / 1000,
                            model = modelId,
                            choices = listOf(
                                ChunkChoice(index = 0, delta = ChunkDelta(), finish_reason = "stop")
                            )
                        )
                        val finishJson = Json.encodeToString(ChatCompletionChunk.serializer(), finishChunk)
                        writeStringUtf8("data: \$finishJson\\n\\n")
                        writeStringUtf8("data: [DONE]\\n\\n")
                        flush()
                    }
                } else {
                    val fullResponse = EngineManager.instance.generateTokens(prompt)
                    ContextManager.appendMessages(conversation, listOf(ChatMessage("assistant", fullResponse)))
                    val respId = "chatcmpl-\${System.currentTimeMillis()}"
                    val response = ChatCompletionResponse(
                        id = respId,
                        created = System.currentTimeMillis() / 1000,
                        model = modelId,
                        choices = listOf(
                            Choice(
                                index = 0,
                                message = ChatMessage("assistant", fullResponse),
                                finish_reason = "stop"
                            )
                        )
                    )
                    call.respond(response)
                }
            }

            get("/metrics") {
                val am = androidContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val pids = intArrayOf(android.os.Process.myPid())
                val memInfo = am.getProcessMemoryInfo(pids)
                val ramMb = if (memInfo.isNotEmpty()) memInfo[0].totalPss / 1024L else 0L

                val metrics = MetricsResponse(
                    ram_used_mb = ramMb,
                    tokens_per_second = MetricsManager.tokensPerSecond,
                    backend = MetricsManager.backend,
                    model = MetricsManager.currentModelName
                )
                call.respond(metrics)
            }
        }
    }
    engine.start(wait = false)
    return engine
}
