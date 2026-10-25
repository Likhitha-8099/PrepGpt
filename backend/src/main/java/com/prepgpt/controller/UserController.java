package com.prepgpt.controller;

import com.prepgpt.dto.UserSummaryResponse;
import com.prepgpt.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management API", description = "Endpoints for user details")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get user summary", description = "Protected endpoint returning current user summary without sensitive data")
    public ResponseEntity<UserSummaryResponse> getCurrentUserSummary(Authentication authentication) {
        UserSummaryResponse response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
