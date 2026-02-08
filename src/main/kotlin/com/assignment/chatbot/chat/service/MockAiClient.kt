package com.assignment.chatbot.chat.service

import com.assignment.chatbot.chat.dto.ChatMessage
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.Duration

@Component
@ConditionalOnExpression("'\${openai.api-key:}'.trim() == ''")
class MockAiClient : AiClient {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("=== MockAiClient 활성화됨 (OpenAI API Key 없음) ===")
    }

    override fun generateResponse(prompt: String, history: List<ChatMessage>, model: String?): String {
        return "[Mock] 질문: \"$prompt\" | 컨텍스트 ${history.size}개 | 모델: ${model ?: "default"}"
    }

    override fun generateStreamResponse(prompt: String, history: List<ChatMessage>, model: String?): Flux<String> {
        val tokens = "[Mock] 스트리밍 응답을 생성 중입니다...".split(" ")
        return Flux.fromIterable(tokens)
            .delayElements(Duration.ofMillis(100))
            .map { "$it " }
    }
}
