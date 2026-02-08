package com.assignment.chatbot.global.exception

import org.springframework.http.HttpStatus

abstract class BusinessException(
    val httpStatus: HttpStatus,
    val errorCode: String,
    override val message: String
) : RuntimeException(message)

class UnauthorizedException(message: String = "인증이 필요합니다.")
    : BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_001", message)

class ForbiddenException(message: String = "접근 권한이 없습니다.")
    : BusinessException(HttpStatus.FORBIDDEN, "AUTH_002", message)

class DuplicateEmailException(email: String)
    : BusinessException(HttpStatus.CONFLICT, "USER_001", "이미 존재하는 이메일입니다: $email")

class InvalidCredentialsException
    : BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다.")

class ResourceNotFoundException(resource: String, id: Any)
    : BusinessException(HttpStatus.NOT_FOUND, "RESOURCE_001", "$resource(id=$id)를 찾을 수 없습니다.")

class DuplicateFeedbackException(chatId: Long, userId: Long)
    : BusinessException(HttpStatus.CONFLICT, "FEEDBACK_001", "이미 피드백이 존재합니다. chatId=$chatId, userId=$userId")

class AiServiceException(message: String)
    : BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_001", message)
