package com.prepgpt.controller;

import com.prepgpt.dto.*;
import com.prepgpt.entity.InterviewMode;
import com.prepgpt.service.InterviewAIService;
import com.prepgpt.service.InterviewEngineService;
import com.prepgpt.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interview-sessions")
@Tag(name = "Interview Session API", description = "Endpoints for configuring, starting, running, and reviewing candidate interview sessions (Question Bank and AI Mode)")
public class InterviewSessionController {

    private final InterviewSessionService sessionService;
    private final InterviewEngineService engineService;
    private final InterviewAIService interviewAIService;

    public InterviewSessionController(InterviewSessionService sessionService,
                                      InterviewEngineService engineService,
                                      InterviewAIService interviewAIService) {
        this.sessionService = sessionService;
        this.engineService = engineService;
        this.interviewAIService = interviewAIService;
    }

    @PostMapping
    @Operation(summary = "Configure new interview session", description = "Creates a CONFIGURED interview session for the authenticated student (Question Bank or AI Mode)")
    public ResponseEntity<InterviewSessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody CreateInterviewSessionRequest request) {
        InterviewSessionResponse response = sessionService.createSession(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get candidate interview sessions", description = "Returns all interview sessions owned by the authenticated student")
    public ResponseEntity<List<InterviewSessionResponse>> getUserSessions(Authentication authentication) {
        List<InterviewSessionResponse> sessions = sessionService.getUserSessions(authentication.getName());
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single interview session details", description = "Fetches specific interview session by ID if owned by authenticated student with question/answer history and evaluations")
    public ResponseEntity<InterviewSessionResponse> getUserSessionById(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        InterviewSessionResponse base = sessionService.getUserSessionById(authentication.getName(), sessionId);
        if (isAIMode(base.getMode())) {
            return ResponseEntity.ok(interviewAIService.getAISessionDetails(authentication.getName(), sessionId));
        }
        return ResponseEntity.ok(engineService.getSessionDetails(authentication.getName(), sessionId));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start configured interview session", description = "Transitions session to IN_PROGRESS and initializes questions based on session mode")
    public ResponseEntity<InterviewSessionResponse> startSession(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        InterviewSessionResponse base = sessionService.getUserSessionById(authentication.getName(), sessionId);
        if (isAIMode(base.getMode())) {
            return ResponseEntity.ok(interviewAIService.startAISession(authentication.getName(), sessionId));
        }
        return ResponseEntity.ok(engineService.startSession(authentication.getName(), sessionId));
    }

    @GetMapping("/{id}/current-question")
    @Operation(summary = "Get current unanswered question", description = "Returns the first unanswered question for an IN_PROGRESS interview session")
    public ResponseEntity<QuestionResponse> getCurrentQuestion(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        InterviewSessionResponse base = sessionService.getUserSessionById(authentication.getName(), sessionId);
        if (isAIMode(base.getMode())) {
            return ResponseEntity.ok(interviewAIService.getCurrentAIQuestion(authentication.getName(), sessionId));
        }
        return ResponseEntity.ok(engineService.getCurrentQuestion(authentication.getName(), sessionId));
    }

    @PostMapping("/{id}/questions/{questionId}/answer")
    @Operation(summary = "Submit answer for question", description = "Stores candidate answer and evaluates response (AI mode evaluates with LLM)")
    public ResponseEntity<AnswerResponse> submitAnswer(
            Authentication authentication,
            @PathVariable("id") Long sessionId,
            @PathVariable("questionId") Long questionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        InterviewSessionResponse base = sessionService.getUserSessionById(authentication.getName(), sessionId);
        if (isAIMode(base.getMode())) {
            return ResponseEntity.ok(interviewAIService.submitAIAnswer(authentication.getName(), sessionId, questionId, request));
        }
        return ResponseEntity.ok(engineService.submitAnswer(authentication.getName(), sessionId, questionId, request));
    }

    @GetMapping("/{id}/next-question")
    @Operation(summary = "Get next unanswered question", description = "Returns the next unanswered question and progress")
    public ResponseEntity<QuestionResponse> getNextQuestion(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        InterviewSessionResponse base = sessionService.getUserSessionById(authentication.getName(), sessionId);
        if (isAIMode(base.getMode())) {
            return ResponseEntity.ok(interviewAIService.getCurrentAIQuestion(authentication.getName(), sessionId));
        }
        return ResponseEntity.ok(engineService.getNextQuestion(authentication.getName(), sessionId));
    }

    private boolean isAIMode(InterviewMode mode) {
        return mode == InterviewMode.AI || mode == InterviewMode.RESUME || mode == InterviewMode.PROJECT || mode == InterviewMode.JOB_DESCRIPTION;
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get interview session status and progress", description = "Returns current question order, answered count, total count, and progress percentage")
    public ResponseEntity<InterviewStatusResponse> getInterviewStatus(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        return ResponseEntity.ok(engineService.getInterviewStatus(authentication.getName(), sessionId));
    }

    @PostMapping("/{id}/abandon")
    @Operation(summary = "Abandon interview session", description = "Transitions session status to ABANDONED")
    public ResponseEntity<InterviewSessionResponse> abandonSession(
            Authentication authentication,
            @PathVariable("id") Long sessionId) {
        InterviewSessionResponse response = sessionService.abandonSession(authentication.getName(), sessionId);
        return ResponseEntity.ok(response);
    }
}
