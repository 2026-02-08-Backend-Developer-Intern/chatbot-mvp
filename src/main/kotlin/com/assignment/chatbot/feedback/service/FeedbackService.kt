package com.assignment.chatbot.feedback.service

import com.assignment.chatbot.chat.repository.ChatRepository
import com.assignment.chatbot.feedback.dto.FeedbackCreateRequest
import com.assignment.chatbot.feedback.dto.FeedbackResponse
import com.assignment.chatbot.feedback.dto.FeedbackStatusUpdateRequest
import com.assignment.chatbot.feedback.entity.Feedback
import com.assignment.chatbot.feedback.repository.FeedbackRepository
import com.assignment.chatbot.global.exception.DuplicateFeedbackException
import com.assignment.chatbot.global.exception.ForbiddenException
import com.assignment.chatbot.global.exception.ResourceNotFoundException
import com.assignment.chatbot.user.entity.Role
import com.assignment.chatbot.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createFeedback(userId: Long, userRole: String, request: FeedbackCreateRequest): FeedbackResponse {
        val chat = chatRepository.findById(request.chatId)
            .orElseThrow { ResourceNotFoundException("Chat", request.chatId) }

        // 중복 체크: 같은 유저가 같은 대화에 피드백을 이미 남겼는지
        if (feedbackRepository.existsByChatIdAndUserId(request.chatId, userId)) {
            throw DuplicateFeedbackException(request.chatId, userId)
        }

        // MEMBER는 자기 대화에만 피드백 가능
        if (userRole == Role.MEMBER.name && chat.thread.user.id != userId) {
            throw ForbiddenException("자신의 대화에만 피드백을 남길 수 있습니다.")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        val feedback = feedbackRepository.save(
            Feedback(chat = chat, user = user, isPositive = request.isPositive)
        )

        return FeedbackResponse.from(feedback)
    }

    /**
     * 피드백 목록 조회
     * - MEMBER: 본인 피드백만
     * - ADMIN: 전체 피드백
     * - isPositive 파라미터로 필터링 가능
     */
    fun listFeedbacks(
        userId: Long,
        userRole: String,
        isPositive: Boolean?,
        pageable: Pageable
    ): Page<FeedbackResponse> {
        val page = if (userRole == Role.ADMIN.name) {
            if (isPositive != null) {
                feedbackRepository.findAllByIsPositive(isPositive, pageable)
            } else {
                feedbackRepository.findAll(pageable)
            }
        } else {
            if (isPositive != null) {
                feedbackRepository.findAllByUserIdAndIsPositive(userId, isPositive, pageable)
            } else {
                feedbackRepository.findAllByUserId(userId, pageable)
            }
        }

        return page.map { FeedbackResponse.from(it) }
    }

    /** 관리자: 피드백 상태 변경 */
    @Transactional
    fun updateStatus(feedbackId: Long, request: FeedbackStatusUpdateRequest): FeedbackResponse {
        val feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow { ResourceNotFoundException("Feedback", feedbackId) }

        feedback.status = request.status
        return FeedbackResponse.from(feedback)
    }
}
