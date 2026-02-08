package com.assignment.chatbot.global.config

import com.assignment.chatbot.user.entity.Role
import com.assignment.chatbot.user.entity.User
import com.assignment.chatbot.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class DataInitializer {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun initData(userRepository: UserRepository, passwordEncoder: PasswordEncoder) = CommandLineRunner {
        if (userRepository.count() == 0L) {
            val admin = User(
                email = "admin@chatbot.com",
                password = passwordEncoder.encode("admin1234"),
                name = "관리자",
                role = Role.ADMIN
            )
            val member = User(
                email = "user@chatbot.com",
                password = passwordEncoder.encode("user1234"),
                name = "테스트유저",
                role = Role.MEMBER
            )
            userRepository.saveAll(listOf(admin, member))
            log.info("=== 초기 계정 시딩 완료: admin@chatbot.com / user@chatbot.com ===")
        }
    }
}
