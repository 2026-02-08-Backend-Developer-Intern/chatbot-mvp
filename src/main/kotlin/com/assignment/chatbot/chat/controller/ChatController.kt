package com.assignment.chatbot.chat.controller

import com.assignment.chatbot.chat.dto.ChatRequest
import com.assignment.chatbot.chat.dto.ChatResponse
import com.assignment.chatbot.chat.dto.ThreadResponse
import com.assignment.chatbot.chat.service.ChatService
import com.assignment.chatbot.chat.service.ThreadService
import com.assignment.chatbot.global.dto.ApiResponse
import com.assignment.chatbot.global.dto.PageResponse
import com.assignment.chatbot.global.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val chatService: ChatService,
    private val threadService: ThreadService
) {

    /** 대화 생성 (동기) */
    @PostMapping
    fun chat(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: ChatRequest
    ): ResponseEntity<ApiResponse<ChatResponse>> {
        if (request.isStreaming) {
            return ResponseEntity.badRequest().body(
                ApiResponse.fail("CHAT_001", "스트리밍 요청은 POST /api/chats/stream을 사용해주세요.")
            )
        }
        val response = chatService.chat(userDetails.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response))
    }

    /** 대화 생성 (SSE 스트리밍) */
    @PostMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun chatStream(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: ChatRequest
    ): Flux<String> {
        val (_, stream) = chatService.chatStream(userDetails.userId, request)
        return stream
    }

    /**
     * 대화 목록 조회 (스레드 단위 그룹화, 페이지네이션)
     * - MEMBER: 자신의 스레드만
     * - ADMIN: 모든 스레드
     */
    @GetMapping
    fun getChats(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "DESC") sort: String
    ): ResponseEntity<ApiResponse<PageResponse<ThreadResponse>>> {
        val direction = if (sort.uppercase() == "ASC") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"))

        val threads = threadService.getThreadsPaged(userDetails.userId, userDetails.role, pageable)
        val response = PageResponse.from(threads.map { ThreadResponse.from(it) })

        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    /** 특정 스레드 상세 조회 */
    @GetMapping("/threads/{threadId}")
    fun getThread(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable threadId: Long
    ): ResponseEntity<ApiResponse<ThreadResponse>> {
        val thread = threadService.getThreadById(threadId)
        return ResponseEntity.ok(ApiResponse.ok(ThreadResponse.from(thread)))
    }

    /** 스레드 삭제 (본인 스레드만) */
    @DeleteMapping("/threads/{threadId}")
    fun deleteThread(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable threadId: Long
    ): ResponseEntity<ApiResponse<String>> {
        threadService.deleteThread(threadId, userDetails.userId)
        return ResponseEntity.ok(ApiResponse.ok("스레드가 삭제되었습니다."))
    }
}
