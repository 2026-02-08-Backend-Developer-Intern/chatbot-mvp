package com.assignment.chatbot.analysis.service

import com.assignment.chatbot.chat.entity.Chat
import com.assignment.chatbot.chat.repository.ChatRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class CsvExportService(
    private val chatRepository: ChatRepository
) {

    private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun exportTodayChatsToCsv(): String {
        val today = LocalDate.now()
        val chats = chatRepository.findAllTodayWithDetails(today.atStartOfDay(), today.atTime(LocalTime.MAX))
        return buildCsv(chats)
    }

    private fun buildCsv(chats: List<Chat>): String {
        val sb = StringBuilder()
        sb.appendLine("chat_id,thread_id,user_email,user_name,question,answer,model,created_at")

        chats.forEach { chat ->
            sb.append(chat.id).append(',')
            sb.append(chat.thread.id).append(',')
            sb.append(escape(chat.thread.user.email)).append(',')
            sb.append(escape(chat.thread.user.name)).append(',')
            sb.append(escape(chat.question)).append(',')
            sb.append(escape(chat.answer)).append(',')
            sb.append(escape(chat.model)).append(',')
            sb.appendLine(chat.createdAt.format(dtf))
        }

        return sb.toString()
    }

    private fun escape(value: String): String {
        return if (value.contains(',') || value.contains('\n') || value.contains('"')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
