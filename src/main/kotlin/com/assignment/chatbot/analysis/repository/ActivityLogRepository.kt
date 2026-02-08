package com.assignment.chatbot.analysis.repository

import com.assignment.chatbot.analysis.entity.ActivityLog
import com.assignment.chatbot.analysis.entity.ActivityType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface ActivityLogRepository : JpaRepository<ActivityLog, Long> {
    fun countByActivityTypeAndCreatedAtBetween(
        activityType: ActivityType,
        start: LocalDateTime,
        end: LocalDateTime
    ): Long
}
