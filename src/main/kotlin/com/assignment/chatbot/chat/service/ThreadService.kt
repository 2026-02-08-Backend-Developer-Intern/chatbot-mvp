package com.assignment.chatbot.chat.service

import com.assignment.chatbot.chat.entity.Thread
import com.assignment.chatbot.chat.repository.ThreadRepository
import com.assignment.chatbot.global.exception.ForbiddenException
import com.assignment.chatbot.global.exception.ResourceNotFoundException
import com.assignment.chatbot.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ThreadService(
    private val threadRepository: ThreadRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val THREAD_TIMEOUT_MINUTES = 30L
    }

    @Transactional
    fun getOrCreateThread(user: User): Thread {
        val now = LocalDateTime.now()
        val latestThread = threadRepository.findFirstByUserIdOrderByLastMessageAtDesc(user.id)

        if (latestThread == null || isThreadExpired(latestThread, now)) {
            log.info("새 Thread 생성 - userId={}, 사유={}",
                user.id, if (latestThread == null) "첫 질문" else "30분 타임아웃")
            return threadRepository.save(Thread(user = user))
        }

        latestThread.touch()
        return latestThread
    }

    fun isThreadExpired(thread: Thread, now: LocalDateTime): Boolean {
        return Duration.between(thread.lastMessageAt, now).toMinutes() >= THREAD_TIMEOUT_MINUTES
    }

    fun getThreadsPaged(userId: Long, role: String, pageable: Pageable): Page<Thread> {
        return if (role == "ADMIN") {
            threadRepository.findAll(pageable)
        } else {
            threadRepository.findAllByUserId(userId, pageable)
        }
    }

    fun getThreadById(threadId: Long): Thread {
        return threadRepository.findById(threadId)
            .orElseThrow { ResourceNotFoundException("Thread", threadId) }
    }

    @Transactional
    fun deleteThread(threadId: Long, userId: Long) {
        val thread = getThreadById(threadId)

        if (thread.user.id != userId) {
            throw ForbiddenException("자신이 생성한 스레드만 삭제할 수 있습니다.")
        }

        threadRepository.delete(thread)
        log.info("Thread 삭제 - threadId={}, userId={}", threadId, userId)
    }
}
