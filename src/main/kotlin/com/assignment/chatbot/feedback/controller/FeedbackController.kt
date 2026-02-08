package com.assignment.chatbot.feedback.controller

import com.assignment.chatbot.feedback.dto.FeedbackCreateRequest
import com.assignment.chatbot.feedback.dto.FeedbackResponse
import com.assignment.chatbot.feedback.dto.FeedbackStatusUpdateRequest
import com.assignment.chatbot.feedback.service.FeedbackService
import com.assignment.chatbot.global.dto.ApiResponse
import com.assignment.chatbot.global.dto.PageResponse
import com.assignment.chatbot.global.security.CustomUserDetails
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feedbacks")
class FeedbackController(
    private val feedbackService: FeedbackService
) {

    /** 피드백 생성 */
    @PostMapping
    fun createFeedback(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody request: FeedbackCreateRequest
    ): ResponseEntity<ApiResponse<FeedbackResponse>> {
        val feedback = feedbackService.createFeedback(
            userId = userDetails.userId,
            userRole = userDetails.role,
            request = request
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feedback))
    }

    /**
     * 피드백 목록 조회 (페이지네이션, 정렬, 필터)
     * - MEMBER: 본인 피드백만
     * - ADMIN: 전체
     */
    @GetMapping
    fun listFeedbacks(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "DESC") sort: String,
        @RequestParam(required = false) isPositive: Boolean?
    ): ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>> {
        val direction = if (sort.uppercase() == "ASC") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"))

        val feedbacks = feedbackService.listFeedbacks(
            userId = userDetails.userId,
            userRole = userDetails.role,
            isPositive = isPositive,
            pageable = pageable
        )

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(feedbacks)))
    }

    /** 피드백 상태 변경 (관리자 전용) */
    @PatchMapping("/{feedbackId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateFeedbackStatus(
        @PathVariable feedbackId: Long,
        @RequestBody request: FeedbackStatusUpdateRequest
    ): ResponseEntity<ApiResponse<FeedbackResponse>> {
        val feedback = feedbackService.updateStatus(feedbackId, request)
        return ResponseEntity.ok(ApiResponse.ok(feedback))
    }
}
