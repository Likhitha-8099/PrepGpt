package com.prepgpt.controller;

import com.prepgpt.dto.DashboardResponse;
import com.prepgpt.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Student Dashboard API", description = "Endpoints for student dashboard metrics and interview history overview")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get student dashboard summary", description = "Returns identity, profile summary, interview statistics, and recent interview sessions")
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        DashboardResponse dashboard = dashboardService.getDashboardSummary(authentication.getName());
        return ResponseEntity.ok(dashboard);
    }
}
