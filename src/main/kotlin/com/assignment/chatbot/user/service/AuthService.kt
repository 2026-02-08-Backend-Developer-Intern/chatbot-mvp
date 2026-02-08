package com.assignment.chatbot.user.service

import com.assignment.chatbot.analysis.entity.ActivityLog
import com.assignment.chatbot.analysis.entity.ActivityType
import com.assignment.chatbot.analysis.repository.ActivityLogRepository
import com.assignment.chatbot.global.exception.DuplicateEmailException
import com.assignment.chatbot.global.exception.InvalidCredentialsException
import com.assignment.chatbot.global.security.JwtTokenProvider
import com.assignment.chatbot.user.dto.*
import com.assignment.chatbot.user.entity.User
import com.assignment.chatbot.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val activityLogRepository: ActivityLogRepository
) {

    @Transactional
    fun signup(request: SignupRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEmailException(request.email)
        }

        val user = userRepository.save(
            User(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                name = request.name,
                role = request.role
            )
        )

        activityLogRepository.save(ActivityLog(userId = user.id, activityType = ActivityType.SIGNUP))

        return UserResponse.from(user)
    }

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        activityLogRepository.save(ActivityLog(userId = user.id, activityType = ActivityType.LOGIN))

        val token = jwtTokenProvider.generateToken(
            userId = user.id,
            email = user.email,
            role = user.role.name
        )

        return LoginResponse(accessToken = token, user = UserResponse.from(user))
    }
}
