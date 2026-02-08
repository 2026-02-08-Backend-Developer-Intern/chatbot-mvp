package com.assignment.chatbot.chat.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "chats",
    indexes = [Index(name = "idx_chat_thread_created", columnList = "thread_id, created_at")]
)
class Chat(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    val thread: Thread,

    @Column(nullable = false, columnDefinition = "TEXT")
    val question: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var answer: String = "",

    @Column(nullable = false)
    val model: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
