package com.assignment.chatbot.chat.service

import com.assignment.chatbot.analysis.entity.ActivityLog
import com.assignment.chatbot.analysis.entity.ActivityType
import com.assignment.chatbot.analysis.repository.ActivityLogRepository
import com.assignment.chatbot.chat.dto.ChatMessage
import com.assignment.chatbot.chat.dto.ChatRequest
import com.assignment.chatbot.chat.dto.ChatResponse
import com.assignment.chatbot.chat.entity.Chat
import com.assignment.chatbot.chat.repository.ChatRepository
import com.assignment.chatbot.global.exception.ResourceNotFoundException
import com.assignment.chatbot.user.entity.User
import com.assignment.chatbot.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatRepository: ChatRepository,
    private val threadService: ThreadService,
    private val aiClient: AiClient,
    private val userRepository: UserRepository,
    private val activityLogRepository: ActivityLogRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun chat(userId: Long, request: ChatRequest): ChatResponse {
        val user = findUserOrThrow(userId)
        val thread = threadService.getOrCreateThread(user)
        val history = buildChatHistory(thread.id)

        val answer = aiClient.generateResponse(
            prompt = request.question,
            history = history,
            model = request.model
        )

        val chat = chatRepository.save(
            Chat(
                thread = thread,
                question = request.question,
                answer = answer,
                model = request.model ?: "default"
            )
        )

        activityLogRepository.save(ActivityLog(userId = userId, activityType = ActivityType.CHAT))

        return ChatResponse.from(chat)
    }

    @Transactional
    fun chatStream(userId: Long, request: ChatRequest): Pair<Long, Flux<String>> {
        val user = findUserOrThrow(userId)
        val thread = threadService.getOrCreateThread(user)
        val history = buildChatHistory(thread.id)

        val chat = chatRepository.save(
            Chat(
                thread = thread,
                question = request.question,
                answer = "",
                model = request.model ?: "default"
            )
        )

        activityLogRepository.save(ActivityLog(userId = userId, activityType = ActivityType.CHAT))

        val answerBuffer = StringBuilder()

        val stream = aiClient.generateStreamResponse(
            prompt = request.question,
            history = history,
            model = request.model
        )
            .doOnNext { token -> answerBuffer.append(token) }
            .doOnComplete {
                chat.answer = answerBuffer.toString()
                chatRepository.save(chat)
            }

        return Pair(chat.id, stream)
    }

    private fun buildChatHistory(threadId: Long): List<ChatMessage> {
        return chatRepository.findAllByThreadIdOrderByCreatedAtAsc(threadId)
            .flatMap { chat ->
                listOf(
                    ChatMessage(role = "user", content = chat.question),
                    ChatMessage(role = "assistant", content = chat.answer)
                )
            }
    }

    private fun findUserOrThrow(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }
    }
}
