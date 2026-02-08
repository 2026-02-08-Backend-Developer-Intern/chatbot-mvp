package com.assignment.chatbot.user.entity

import jakarta.persistence.*
import java.time.LocalDateTime

enum class Role {
    MEMBER,
    ADMIN
}

@Entity
@Table(name = "users")
class User(

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.MEMBER,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)
