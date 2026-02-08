package com.assignment.chatbot.chat.service

import com.assignment.chatbot.chat.dto.ChatMessage
import com.assignment.chatbot.global.config.OpenAiProperties
import com.assignment.chatbot.global.exception.AiServiceException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.time.Duration

@Component
@ConditionalOnExpression("'\${openai.api-key:}'.trim() != ''")
class OpenAiClient(
    private val openAiWebClient: WebClient,
    private val openAiProperties: OpenAiProperties,
    private val objectMapper: ObjectMapper
) : AiClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun generateResponse(prompt: String, history: List<ChatMessage>, model: String?): String {
        val requestBody = buildRequestBody(prompt, history, model, stream = false)

        try {
            val responseBody = openAiWebClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(openAiProperties.timeoutSeconds))
                .block() ?: throw AiServiceException("OpenAI 응답이 비어있습니다.")

            val jsonNode = objectMapper.readTree(responseBody)
            return jsonNode["choices"][0]["message"]["content"].asText()
        } catch (ex: AiServiceException) {
            throw ex
        } catch (ex: Exception) {
            log.error("OpenAI API 호출 실패", ex)
            throw AiServiceException("AI 서비스 호출 중 오류가 발생했습니다: ${ex.message}")
        }
    }

    override fun generateStreamResponse(prompt: String, history: List<ChatMessage>, model: String?): Flux<String> {
        val requestBody = buildRequestBody(prompt, history, model, stream = true)

        return openAiWebClient.post()
            .uri("/v1/chat/completions")
            .bodyValue(requestBody)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(String::class.java)
            .timeout(Duration.ofSeconds(openAiProperties.timeoutSeconds))
            .filter { it != "[DONE]" }
            .map { chunk ->
                try {
                    val jsonNode = objectMapper.readTree(chunk)
                    jsonNode["choices"]?.get(0)?.get("delta")?.get("content")?.asText()
                } catch (ex: Exception) {
                    log.debug("스트리밍 청크 파싱 스킵: {}", chunk)
                    null
                }
            }
            .filter { it != null }
            .cast(String::class.java)
            .onErrorResume { ex ->
                log.error("OpenAI 스트리밍 오류", ex)
                Flux.error(AiServiceException("AI 스트리밍 응답 중 오류 발생"))
            }
    }

    private fun buildRequestBody(
        prompt: String,
        history: List<ChatMessage>,
        model: String?,
        stream: Boolean
    ): Map<String, Any> {
        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to "You are a helpful AI assistant."))
        history.forEach { messages.add(mapOf("role" to it.role, "content" to it.content)) }
        messages.add(mapOf("role" to "user", "content" to prompt))

        return mapOf(
            "model" to (model ?: openAiProperties.defaultModel),
            "messages" to messages,
            "stream" to stream
        )
    }
}
