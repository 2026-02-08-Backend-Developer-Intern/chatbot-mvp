package com.assignment.chatbot.chat.repository

import com.assignment.chatbot.chat.entity.Chat
import com.assignment.chatbot.chat.entity.Thread
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ThreadRepository : JpaRepository<Thread, Long> {

    fun findFirstByUserIdOrderByLastMessageAtDesc(userId: Long): Thread?

    // 유저 본인의 스레드 (페이지네이션)
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Thread>

    // 관리자: 전체 스레드 (페이지네이션)
    override fun findAll(pageable: Pageable): Page<Thread>
}

interface ChatRepository : JpaRepository<Chat, Long> {

    fun findAllByThreadIdOrderByCreatedAtAsc(threadId: Long): List<Chat>

    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long

    @Query("""
        SELECT c FROM Chat c 
        JOIN FETCH c.thread t 
        JOIN FETCH t.user u 
        WHERE c.createdAt BETWEEN :start AND :end 
        ORDER BY c.createdAt ASC
    """)
    fun findAllTodayWithDetails(start: LocalDateTime, end: LocalDateTime): List<Chat>
}
