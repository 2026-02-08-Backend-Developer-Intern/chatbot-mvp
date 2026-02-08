package com.assignment.chatbot.chat.service

import com.assignment.chatbot.chat.entity.Thread
import com.assignment.chatbot.chat.repository.ThreadRepository
import com.assignment.chatbot.user.entity.Role
import com.assignment.chatbot.user.entity.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ThreadServiceTest {

    @Mock
    lateinit var threadRepository: ThreadRepository

    @InjectMocks
    lateinit var threadService: ThreadService

    private val testUser = User(
        email = "test@example.com",
        password = "encoded",
        name = "테스트",
        role = Role.MEMBER,
        id = 1L
    )

    @Nested
    @DisplayName("getOrCreateThread()")
    inner class GetOrCreateThread {

        @Test
        @DisplayName("첫 질문 -> 새 Thread 생성")
        fun `should create new thread when no previous thread exists`() {
            whenever(threadRepository.findFirstByUserIdOrderByLastMessageAtDesc(1L)).thenReturn(null)
            whenever(threadRepository.save(any<Thread>())).thenAnswer { it.arguments[0] as Thread }

            threadService.getOrCreateThread(testUser)

            verify(threadRepository).save(any<Thread>())
        }

        @Test
        @DisplayName("31분 경과 -> 새 Thread 생성")
        fun `should create new thread when expired`() {
            val expired = Thread(user = testUser, lastMessageAt = LocalDateTime.now().minusMinutes(31), id = 10L)
            whenever(threadRepository.findFirstByUserIdOrderByLastMessageAtDesc(1L)).thenReturn(expired)
            whenever(threadRepository.save(any<Thread>())).thenAnswer { it.arguments[0] as Thread }

            threadService.getOrCreateThread(testUser)

            verify(threadRepository).save(any<Thread>())
        }

        @Test
        @DisplayName("15분 경과 -> 기존 Thread 유지")
        fun `should reuse thread within 30 minutes`() {
            val active = Thread(user = testUser, lastMessageAt = LocalDateTime.now().minusMinutes(15), id = 10L)
            whenever(threadRepository.findFirstByUserIdOrderByLastMessageAtDesc(1L)).thenReturn(active)

            val result = threadService.getOrCreateThread(testUser)

            assertEquals(10L, result.id)
            verify(threadRepository, never()).save(any<Thread>())
        }
    }

    @Nested
    @DisplayName("isThreadExpired() 경계값")
    inner class IsThreadExpired {

        @Test
        @DisplayName("정확히 30분 -> 만료")
        fun `exactly 30 minutes should be expired`() {
            val now = LocalDateTime.now()
            val thread = Thread(user = testUser, lastMessageAt = now.minusMinutes(30))
            assertTrue(threadService.isThreadExpired(thread, now))
        }

        @Test
        @DisplayName("29분 -> 미만료")
        fun `29 minutes should not be expired`() {
            val now = LocalDateTime.now()
            val thread = Thread(user = testUser, lastMessageAt = now.minusMinutes(29))
            assertFalse(threadService.isThreadExpired(thread, now))
        }
    }
}
