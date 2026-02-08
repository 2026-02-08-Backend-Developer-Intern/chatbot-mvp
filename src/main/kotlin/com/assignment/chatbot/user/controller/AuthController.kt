package com.assignment.chatbot.user.controller

import com.assignment.chatbot.global.dto.ApiResponse
import com.assignment.chatbot.user.dto.LoginRequest
import com.assignment.chatbot.user.dto.LoginResponse
import com.assignment.chatbot.user.dto.SignupRequest
import com.assignment.chatbot.user.dto.UserResponse
import com.assignment.chatbot.user.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val user = authService.signup(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(user))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }
}
