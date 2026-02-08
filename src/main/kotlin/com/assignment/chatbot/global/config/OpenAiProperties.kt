package com.assignment.chatbot.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "openai")
data class OpenAiProperties(
    val apiKey: String,
    val baseUrl: String,
    val defaultModel: String,
    val timeoutSeconds: Long
)
