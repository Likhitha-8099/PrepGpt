package com.prepgpt.controller;

import com.prepgpt.dto.CreateProjectRequest;
import com.prepgpt.dto.ProjectResponse;
import com.prepgpt.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Management API", description = "Endpoints for managing candidate software projects and triggering Groq AI technical breakdowns")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create candidate project", description = "Creates a new software project record owned by the authenticated student")
    public ResponseEntity<ProjectResponse> createProject(
            Authentication authentication,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List candidate projects", description = "Returns all software projects owned by the authenticated student")
    public ResponseEntity<List<ProjectResponse>> getUserProjects(Authentication authentication) {
        List<ProjectResponse> projects = projectService.getUserProjects(authentication.getName());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single project details", description = "Fetches specific project record by ID with candidate ownership check")
    public ResponseEntity<ProjectResponse> getProjectById(
            Authentication authentication,
            @PathVariable("id") Long projectId) {
        ProjectResponse response = projectService.getProjectById(authentication.getName(), projectId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update candidate project", description = "Updates details, technologies, or contributions of a candidate project")
    public ResponseEntity<ProjectResponse> updateProject(
            Authentication authentication,
            @PathVariable("id") Long projectId,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(authentication.getName(), projectId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/analyze")
    @Operation(summary = "Trigger Groq AI project technical breakdown", description = "Generates technical architecture summary, key challenges, and targeted interview topics")
    public ResponseEntity<ProjectResponse> analyzeProject(
            Authentication authentication,
            @PathVariable("id") Long projectId) {
        ProjectResponse response = projectService.analyzeProject(authentication.getName(), projectId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete candidate project", description = "Deletes a project record owned by the authenticated student")
    public ResponseEntity<Void> deleteProject(
            Authentication authentication,
            @PathVariable("id") Long projectId) {
        projectService.deleteProject(authentication.getName(), projectId);
        return ResponseEntity.noContent().build();
    }
}
