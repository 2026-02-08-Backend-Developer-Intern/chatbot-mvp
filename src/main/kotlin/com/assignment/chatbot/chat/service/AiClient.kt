package com.assignment.chatbot.chat.service

import com.assignment.chatbot.chat.dto.ChatMessage
import reactor.core.publisher.Flux

/**
 * AI 응답 생성 추상화 인터페이스 (Strategy Pattern + DIP)
 *
 * 구현체: OpenAiClient (실제 API), MockAiClient (테스트용)
 * 확장 시: RagAiClient 등 구현체 추가 후 @Profile/@Qualifier로 전환
 */
interface AiClient {
    fun generateResponse(prompt: String, history: List<ChatMessage>, model: String?): String
    fun generateStreamResponse(prompt: String, history: List<ChatMessage>, model: String?): Flux<String>
}
