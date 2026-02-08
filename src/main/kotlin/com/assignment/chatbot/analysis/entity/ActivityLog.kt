package com.assignment.chatbot.analysis.entity

import jakarta.persistence.*
import java.time.LocalDateTime

enum class ActivityType {
    SIGNUP,
    LOGIN,
    CHAT
}

@Entity
@Table(
    name = "activity_logs",
    indexes = [Index(name = "idx_activity_type_created", columnList = "activity_type, created_at")]
)
class ActivityLog(

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    val activityType: ActivityType,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
