package com.example.server

import com.example.model.ChatMessage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ConversationState(
    val id: String,
    val messages: MutableList<ChatMessage> = mutableListOf()
)

object ContextManager {
    private val conversations = ConcurrentHashMap<String, ConversationState>()
    private const val MAX_TOKENS = 4096

    fun getOrCreateConversation(id: String?): ConversationState {
        val convId = id ?: UUID.randomUUID().toString()
        return conversations.getOrPut(convId) {
            ConversationState(id = convId)
        }
    }

    fun appendMessages(conversation: ConversationState, newMessages: List<ChatMessage>) {
        conversation.messages.addAll(newMessages)
        truncateConversation(conversation)
    }

    private fun truncateConversation(conversation: ConversationState) {
        // Approximate 1 token = 4 chars for truncation
        while (estimateTokens(conversation.messages) > MAX_TOKENS && conversation.messages.size > 1) {
            // retain system prompt if at index 0?
            if (conversation.messages.size > 2 && conversation.messages[0].role == "system") {
                conversation.messages.removeAt(1)
            } else {
                conversation.messages.removeAt(0)
            }
        }
    }

    private fun estimateTokens(messages: List<ChatMessage>): Int {
        var total = 0
        for (m in messages) {
            total += m.content.length / 4 + 10 // rough estimate
        }
        return total
    }

    fun buildPrompt(conversation: ConversationState): String {
        return conversation.messages.joinToString("\n") {
            "<|im_start|>${it.role}\n${it.content}<|im_end|>"
        } + "\n<|im_start|>assistant\n"
    }
}
