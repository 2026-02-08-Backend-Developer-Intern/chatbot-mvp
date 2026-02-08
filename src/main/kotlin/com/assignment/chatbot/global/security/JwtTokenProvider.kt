package com.assignment.chatbot.global.security

import com.assignment.chatbot.global.config.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val secretKey: SecretKey by lazy {
        val decodedKey = Base64.getDecoder().decode(jwtProperties.secret)
        Keys.hmacShaKeyFor(decodedKey)
    }

    fun generateToken(userId: Long, email: String, role: String): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.expirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun getUserId(token: String): Long = getClaims(token).subject.toLong()

    fun getRole(token: String): String = getClaims(token)["role"] as String

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (ex: ExpiredJwtException) {
            log.warn("JWT 만료: {}", ex.message)
            false
        } catch (ex: JwtException) {
            log.warn("JWT 유효하지 않음: {}", ex.message)
            false
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
