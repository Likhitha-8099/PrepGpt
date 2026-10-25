package com.prepgpt.controller;

import com.prepgpt.dto.ResumeResponse;
import com.prepgpt.service.ResumeAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resume Analysis API", description = "Endpoints for uploading, parsing, analyzing, and managing candidate resumes")
public class ResumeController {

    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeController(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload and parse candidate resume", description = "Stores uploaded PDF/DOCX/TXT file on disk, extracts text using Apache Tika, and triggers Groq AI structuring")
    public ResponseEntity<ResumeResponse> uploadResume(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        ResumeResponse response = resumeAnalysisService.uploadAndAnalyzeResume(authentication.getName(), file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List candidate resumes", description = "Returns all uploaded resumes owned by the authenticated student")
    public ResponseEntity<List<ResumeResponse>> getUserResumes(Authentication authentication) {
        List<ResumeResponse> resumes = resumeAnalysisService.getUserResumes(authentication.getName());
        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single resume details", description = "Fetches specific resume details, extracted text, and AI analysis by ID with candidate ownership check")
    public ResponseEntity<ResumeResponse> getResumeById(
            Authentication authentication,
            @PathVariable("id") Long resumeId) {
        ResumeResponse response = resumeAnalysisService.getResumeById(authentication.getName(), resumeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/analyze")
    @Operation(summary = "Trigger Groq AI analysis on resume", description = "Analyzes raw resume text with Groq LLM to extract structured skills, work history, education, and projects")
    public ResponseEntity<ResumeResponse> analyzeResume(
            Authentication authentication,
            @PathVariable("id") Long resumeId) {
        ResumeResponse response = resumeAnalysisService.analyzeResume(authentication.getName(), resumeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/set-primary")
    @Operation(summary = "Mark resume as primary default", description = "Sets target resume as the primary resume for candidate mock interviews")
    public ResponseEntity<ResumeResponse> setPrimaryResume(
            Authentication authentication,
            @PathVariable("id") Long resumeId) {
        ResumeResponse response = resumeAnalysisService.setPrimaryResume(authentication.getName(), resumeId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete candidate resume", description = "Deletes local resume file and database record")
    public ResponseEntity<Void> deleteResume(
            Authentication authentication,
            @PathVariable("id") Long resumeId) {
        resumeAnalysisService.deleteResume(authentication.getName(), resumeId);
        return ResponseEntity.noContent().build();
    }
}
