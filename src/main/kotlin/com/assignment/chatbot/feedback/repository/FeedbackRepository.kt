package com.assignment.chatbot.feedback.repository

import com.assignment.chatbot.feedback.entity.Feedback
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepository : JpaRepository<Feedback, Long> {

    fun existsByChatIdAndUserId(chatId: Long, userId: Long): Boolean

    // MEMBER: 본인 피드백만 (필터 없이)
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Feedback>

    // MEMBER: 본인 피드백 + isPositive 필터
    fun findAllByUserIdAndIsPositive(userId: Long, isPositive: Boolean, pageable: Pageable): Page<Feedback>

    // ADMIN: 전체 피드백 + isPositive 필터
    fun findAllByIsPositive(isPositive: Boolean, pageable: Pageable): Page<Feedback>
}
