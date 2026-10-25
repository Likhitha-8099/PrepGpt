package com.prepgpt.controller;

import com.prepgpt.dto.AnalyticsOverviewResponse;
import com.prepgpt.dto.CategoryAnalyticsResponse;
import com.prepgpt.dto.InterviewReportResponse;
import com.prepgpt.service.ReportAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Interview Reports & Analytics API", description = "Endpoints for candidate interview reports, performance analytics, and category skill metrics")
public class InterviewReportController {

    private final ReportAnalyticsService reportAnalyticsService;

    public InterviewReportController(ReportAnalyticsService reportAnalyticsService) {
        this.reportAnalyticsService = reportAnalyticsService;
    }

    @GetMapping("/reports/sessions/{sessionId}")
    @Operation(summary = "Get detailed report for a completed interview session", description = "Returns overall metrics, multi-attribute score averages, key strengths/weaknesses, adaptive summary, and question-by-question evaluations")
    public ResponseEntity<InterviewReportResponse> getInterviewReport(
            @PathVariable Long sessionId,
            Authentication authentication) {
        InterviewReportResponse response = reportAnalyticsService.getInterviewReport(authentication.getName(), sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/overview")
    @Operation(summary = "Get historical analytics overview", description = "Returns candidate interview statistics, overall score trends over time, completion rates, and strongest/weakest categories")
    public ResponseEntity<AnalyticsOverviewResponse> getAnalyticsOverview(Authentication authentication) {
        AnalyticsOverviewResponse response = reportAnalyticsService.getAnalyticsOverview(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/categories")
    @Operation(summary = "Get historical category skill performance analytics", description = "Returns aggregated performance metrics by technical category across candidate's historical interview sessions")
    public ResponseEntity<List<CategoryAnalyticsResponse>> getCategoryAnalytics(Authentication authentication) {
        List<CategoryAnalyticsResponse> response = reportAnalyticsService.getCategoryAnalytics(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
