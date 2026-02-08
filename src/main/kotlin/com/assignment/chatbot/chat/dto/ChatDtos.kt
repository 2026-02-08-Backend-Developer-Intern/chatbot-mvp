package com.assignment.chatbot.chat.dto

import com.assignment.chatbot.chat.entity.Chat
import com.assignment.chatbot.chat.entity.Thread
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class ChatRequest(
    @field:NotBlank(message = "질문 내용은 필수입니다")
    val question: String,
    val isStreaming: Boolean = false,
    val model: String? = null
)

data class ChatResponse(
    val chatId: Long,
    val threadId: Long,
    val question: String,
    val answer: String,
    val model: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(chat: Chat): ChatResponse = ChatResponse(
            chatId = chat.id,
            threadId = chat.thread.id,
            question = chat.question,
            answer = chat.answer,
            model = chat.model,
            createdAt = chat.createdAt
        )
    }
}

data class ThreadResponse(
    val threadId: Long,
    val userId: Long,
    val lastMessageAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val chats: List<ChatResponse>
) {
    companion object {
        fun from(thread: Thread): ThreadResponse = ThreadResponse(
            threadId = thread.id,
            userId = thread.user.id,
            lastMessageAt = thread.lastMessageAt,
            createdAt = thread.createdAt,
            chats = thread.chats.map { ChatResponse.from(it) }
        )
    }
}

data class ChatMessage(
    val role: String,
    val content: String
)
