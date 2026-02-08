package com.assignment.chatbot.chat.entity

import com.assignment.chatbot.user.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "threads",
    indexes = [Index(name = "idx_thread_user_last_message", columnList = "user_id, last_message_at DESC")]
)
class Thread(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "last_message_at", nullable = false)
    var lastMessageAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "thread", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val chats: MutableList<Chat> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
) {
    fun touch() {
        this.lastMessageAt = LocalDateTime.now()
    }
}
