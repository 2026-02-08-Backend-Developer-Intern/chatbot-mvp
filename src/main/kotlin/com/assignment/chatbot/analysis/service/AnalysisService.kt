package com.assignment.chatbot.analysis.service

import com.assignment.chatbot.analysis.dto.DailyActivityResponse
import com.assignment.chatbot.analysis.entity.ActivityType
import com.assignment.chatbot.analysis.repository.ActivityLogRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

@Service
class AnalysisService(
    private val activityLogRepository: ActivityLogRepository
) {

    fun getDailyActivity(): DailyActivityResponse {
        val today = LocalDate.now()
        val start = today.atStartOfDay()
        val end = today.atTime(LocalTime.MAX)

        return DailyActivityResponse(
            date = today,
            signupCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.SIGNUP, start, end),
            loginCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.LOGIN, start, end),
            chatCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(ActivityType.CHAT, start, end)
        )
    }
}
