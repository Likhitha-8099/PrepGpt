package com.prepgpt.controller;

import com.prepgpt.dto.*;
import com.prepgpt.service.InterviewAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interview-sessions")
@Tag(name = "AI Interviewer API", description = "Endpoints for dynamic AI question generation, response evaluation, and follow-up management")
public class InterviewAIController {

    private final InterviewAIService interviewAIService;

    public InterviewAIController(InterviewAIService interviewAIService) {
        this.interviewAIService = interviewAIService;
    }

    @PostMapping("/{id}/ai/start")
    @Operation(summary = "Start AI interview session", description = "Initializes AI interview and generates the first AI question")
    public ResponseEntity<InterviewSessionResponse> startAISession(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        InterviewSessionResponse response = interviewAIService.startAISession(authentication.getName(), sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/ai/current")
    @Operation(summary = "Get current AI question", description = "Returns current active AI question or dynamically generates the next question")
    public ResponseEntity<QuestionResponse> getCurrentAIQuestion(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        QuestionResponse response = interviewAIService.getCurrentAIQuestion(authentication.getName(), sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ai/questions/{questionId}/answer")
    @Operation(summary = "Submit answer for AI evaluation", description = "Persists candidate answer, evaluates response with AI, and returns structured feedback and follow-up")
    public ResponseEntity<AnswerResponse> submitAIAnswer(
            Authentication authentication,
            @PathVariable("id") Long sessionId,
            @PathVariable("questionId") Long questionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        AnswerResponse response = interviewAIService.submitAIAnswer(authentication.getName(), sessionId, questionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/ai/details")
    @Operation(summary = "Get AI session details with evaluations", description = "Returns complete interview details including questions, candidate answers, and AI evaluations")
    public ResponseEntity<InterviewSessionResponse> getAISessionDetails(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        InterviewSessionResponse response = interviewAIService.getAISessionDetails(authentication.getName(), sessionId);
        return ResponseEntity.ok(response);
    }
}
