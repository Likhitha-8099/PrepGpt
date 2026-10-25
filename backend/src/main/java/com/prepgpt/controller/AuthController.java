package com.prepgpt.controller;

import com.prepgpt.dto.JwtAuthResponse;
import com.prepgpt.dto.LoginRequest;
import com.prepgpt.dto.RegisterRequest;
import com.prepgpt.dto.UserSummaryResponse;
import com.prepgpt.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication API", description = "Endpoints for student registration, login, and token verification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new student", description = "Creates a new user account with ROLE_STUDENT and initializes student profile")
    public ResponseEntity<UserSummaryResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserSummaryResponse response = authService.register(registerRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user credentials and returns JWT bearer token")
    public ResponseEntity<JwtAuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user summary", description = "Returns identity details of the authenticated user")
    public ResponseEntity<UserSummaryResponse> getCurrentUser(Authentication authentication) {
        UserSummaryResponse response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
