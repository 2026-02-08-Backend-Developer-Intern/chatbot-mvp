package com.assignment.chatbot.feedback.entity

import com.assignment.chatbot.chat.entity.Chat
import com.assignment.chatbot.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime

enum class FeedbackStatus {
    PENDING,
    RESOLVED
}

@Entity
@Table(
    name = "feedbacks",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_feedback_chat_user", columnNames = ["chat_id", "user_id"])
    ]
)
class Feedback(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val chat: Chat,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "is_positive", nullable = false)
    val isPositive: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
