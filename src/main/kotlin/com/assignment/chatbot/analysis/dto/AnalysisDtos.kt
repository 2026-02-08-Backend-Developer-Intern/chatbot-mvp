package com.assignment.chatbot.analysis.dto

import java.time.LocalDate

data class DailyActivityResponse(
    val date: LocalDate,
    val signupCount: Long,
    val loginCount: Long,
    val chatCount: Long
)
