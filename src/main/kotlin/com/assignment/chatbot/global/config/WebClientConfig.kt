package com.assignment.chatbot.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    private val openAiProperties: OpenAiProperties
) {
    @Bean
    fun openAiWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(openAiProperties.baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${openAiProperties.apiKey}")
            .build()
    }
}
