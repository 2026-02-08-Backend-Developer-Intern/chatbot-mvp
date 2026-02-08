package com.assignment.chatbot.feedback.dto

import com.assignment.chatbot.feedback.entity.Feedback
import com.assignment.chatbot.feedback.entity.FeedbackStatus
import java.time.LocalDateTime

data class FeedbackCreateRequest(
    val chatId: Long,
    val isPositive: Boolean
)

data class FeedbackStatusUpdateRequest(
    val status: FeedbackStatus
)

data class FeedbackResponse(
    val id: Long,
    val chatId: Long,
    val userId: Long,
    val isPositive: Boolean,
    val status: FeedbackStatus,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(feedback: Feedback): FeedbackResponse = FeedbackResponse(
            id = feedback.id,
            chatId = feedback.chat.id,
            userId = feedback.user.id,
            isPositive = feedback.isPositive,
            status = feedback.status,
            createdAt = feedback.createdAt
        )
    }
}
