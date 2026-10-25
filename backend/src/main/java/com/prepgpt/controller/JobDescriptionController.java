package com.prepgpt.controller;

import com.prepgpt.dto.CreateJobDescriptionRequest;
import com.prepgpt.dto.JobDescriptionResponse;
import com.prepgpt.entity.JobDescription;
import com.prepgpt.entity.User;
import com.prepgpt.repository.UserRepository;
import com.prepgpt.service.JobDescriptionAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/job-descriptions")
@Tag(name = "Job Description API", description = "Endpoints for parsing, analyzing, and managing candidate job descriptions")
public class JobDescriptionController {

    private final JobDescriptionAnalysisService jdService;
    private final UserRepository userRepository;

    public JobDescriptionController(JobDescriptionAnalysisService jdService, UserRepository userRepository) {
        this.jdService = jdService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create and analyze job description", description = "Parses and extracts skills, responsibilities, and experience level from candidate JD input")
    public ResponseEntity<JobDescriptionResponse> createJobDescription(
            Authentication authentication,
            @Valid @RequestBody CreateJobDescriptionRequest request) {
        User user = getUser(authentication);
        JobDescription jd = jdService.analyzeAndSaveJobDescription(user, request);
        return new ResponseEntity<>(new JobDescriptionResponse(jd), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List candidate job descriptions", description = "Returns all job descriptions owned by the authenticated student")
    public ResponseEntity<List<JobDescriptionResponse>> getUserJobDescriptions(Authentication authentication) {
        User user = getUser(authentication);
        List<JobDescription> list = jdService.getAllJobDescriptionsForUser(user.getId());
        List<JobDescriptionResponse> dtos = list.stream().map(JobDescriptionResponse::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single job description detail", description = "Fetches specific JD record by ID with candidate ownership check")
    public ResponseEntity<JobDescriptionResponse> getJobDescriptionById(
            Authentication authentication,
            @PathVariable("id") Long id) {
        User user = getUser(authentication);
        JobDescription jd = jdService.getJobDescriptionForUser(id, user.getId());
        return ResponseEntity.ok(new JobDescriptionResponse(jd));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete candidate job description", description = "Deletes a job description record owned by the authenticated student")
    public ResponseEntity<Void> deleteJobDescription(
            Authentication authentication,
            @PathVariable("id") Long id) {
        User user = getUser(authentication);
        jdService.deleteJobDescriptionForUser(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
