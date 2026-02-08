package com.assignment.chatbot.global.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<out T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> =
            ApiResponse(success = true, data = data)

        fun <T> created(data: T): ApiResponse<T> =
            ApiResponse(success = true, data = data)

        fun fail(code: String, message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, error = ErrorDetail(code, message))
    }
}

data class ErrorDetail(
    val code: String,
    val message: String
)
