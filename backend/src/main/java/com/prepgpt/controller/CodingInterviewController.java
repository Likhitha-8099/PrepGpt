package com.prepgpt.controller;

import com.prepgpt.dto.*;
import com.prepgpt.entity.User;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.UserRepository;
import com.prepgpt.service.CodingProblemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coding")
public class CodingInterviewController {

    private final CodingProblemService codingProblemService;
    private final UserRepository userRepository;

    public CodingInterviewController(CodingProblemService codingProblemService, UserRepository userRepository) {
        this.codingProblemService = codingProblemService;
        this.userRepository = userRepository;
    }

    @GetMapping("/problems")
    public ResponseEntity<List<CodingProblemResponse>> getAllProblems() {
        return ResponseEntity.ok(codingProblemService.getAllProblems());
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<CodingProblemResponse> getProblemById(@PathVariable Long id) {
        return ResponseEntity.ok(codingProblemService.getProblemById(id));
    }

    @PostMapping("/sessions/{sessionId}/questions/{questionId}/run")
    public ResponseEntity<RunCodeResponse> runCode(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            Authentication authentication,
            @Valid @RequestBody RunCodeRequest request
    ) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));
        RunCodeResponse response = codingProblemService.runCode(sessionId, questionId, user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/questions/{questionId}/submit")
    public ResponseEntity<SubmitCodeResponse> submitCode(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            Authentication authentication,
            @Valid @RequestBody RunCodeRequest request
    ) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", authentication.getName()));
        SubmitCodeResponse response = codingProblemService.submitCode(sessionId, questionId, user.getId(), request);
        return ResponseEntity.ok(response);
    }
}
