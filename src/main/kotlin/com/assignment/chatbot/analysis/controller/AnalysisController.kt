package com.assignment.chatbot.analysis.controller

import com.assignment.chatbot.analysis.dto.DailyActivityResponse
import com.assignment.chatbot.analysis.service.AnalysisService
import com.assignment.chatbot.analysis.service.CsvExportService
import com.assignment.chatbot.global.dto.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/analysis")
class AnalysisController(
    private val analysisService: AnalysisService,
    private val csvExportService: CsvExportService
) {

    @GetMapping("/daily")
    fun getDailyActivity(): ResponseEntity<ApiResponse<DailyActivityResponse>> {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getDailyActivity()))
    }

    @GetMapping("/csv")
    fun downloadCsv(): ResponseEntity<ByteArray> {
        val csv = csvExportService.exportTodayChatsToCsv()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=chats_${LocalDate.now()}.csv")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv.toByteArray(Charsets.UTF_8))
    }
}
