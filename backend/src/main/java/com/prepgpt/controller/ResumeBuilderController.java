package com.prepgpt.controller;

import com.prepgpt.dto.*;
import com.prepgpt.service.ResumeBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resume-builder")
@Tag(name = "ATS Resume Builder API", description = "Endpoints for creating, AI-enhancing, prefilling, and saving ATS-friendly resumes")
public class ResumeBuilderController {

    private final ResumeBuilderService resumeBuilderService;

    public ResumeBuilderController(ResumeBuilderService resumeBuilderService) {
        this.resumeBuilderService = resumeBuilderService;
    }

    @GetMapping("/initial-data")
    @Operation(summary = "Get initial resume builder candidate data", description = "Fetches candidate profile, existing resumes, and projects to prefill the ATS resume builder")
    public ResponseEntity<ResumeBuilderInitialDataResponse> getInitialData(Authentication authentication) {
        ResumeBuilderInitialDataResponse response = resumeBuilderService.getInitialData(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ai-assist")
    @Operation(summary = "Request Groq AI assistance for resume section", description = "Provides AI-assisted improvements for summaries, bullet points, skills suggestions, and JD keyword matching")
    public ResponseEntity<ResumeBuilderAiAssistResponse> processAiAssist(
            Authentication authentication,
            @Valid @RequestBody ResumeBuilderAiAssistRequest request) {
        ResumeBuilderAiAssistResponse response = resumeBuilderService.processAiAssist(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-to-resumes")
    @Operation(summary = "Save generated ATS resume as active candidate resume", description = "Persists the built ATS resume into candidate's resumes for immediate use in mock interview practice")
    public ResponseEntity<ResumeResponse> saveResumeFromBuilder(
            Authentication authentication,
            @Valid @RequestBody ResumeBuilderSaveRequest request) {
        ResumeResponse response = resumeBuilderService.saveResumeFromBuilder(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
