package com.assignment.chatbot.user.dto

import com.assignment.chatbot.user.entity.Role
import com.assignment.chatbot.user.entity.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class SignupRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,

    val role: Role = Role.MEMBER
)

data class LoginRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
            createdAt = user.createdAt
        )
    }
}
