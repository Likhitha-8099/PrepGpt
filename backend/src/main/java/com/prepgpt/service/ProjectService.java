package com.prepgpt.service;

import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.ProjectAnalysisPromptBuilder;
import com.prepgpt.dto.CreateProjectRequest;
import com.prepgpt.dto.ProjectResponse;
import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.Project;
import com.prepgpt.entity.Resume;
import com.prepgpt.entity.User;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.ProjectRepository;
import com.prepgpt.repository.ResumeRepository;
import com.prepgpt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ProjectAnalysisPromptBuilder projectAnalysisPromptBuilder;
    private final GroqAIService groqAIService;

    public ProjectService(ProjectRepository projectRepository,
                          ResumeRepository resumeRepository,
                          UserRepository userRepository,
                          ProjectAnalysisPromptBuilder projectAnalysisPromptBuilder,
                          GroqAIService groqAIService) {
        this.projectRepository = projectRepository;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.projectAnalysisPromptBuilder = projectAnalysisPromptBuilder;
        this.groqAIService = groqAIService;
    }

    @Transactional
    public ProjectResponse createProject(String userEmail, CreateProjectRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = new Project(
                user,
                request.getTitle().trim(),
                request.getDescription(),
                request.getTechnologies(),
                request.getRoleContribution()
        );

        project.setKeyFeatures(request.getKeyFeatures());
        project.setArchitectureDetails(request.getArchitectureDetails());
        project.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : Difficulty.MEDIUM);

        if (request.getResumeId() != null) {
            Resume resume = resumeRepository.findByIdAndUserId(request.getResumeId(), user.getId())
                    .orElse(null);
            project.setResume(resume);
        }

        Project savedProject = projectRepository.save(project);
        return mapToProjectResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(String userEmail, Long projectId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId + " for current user"));

        return mapToProjectResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(String userEmail, Long projectId, CreateProjectRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId + " for current user"));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            project.setTitle(request.getTitle().trim());
        }
        project.setDescription(request.getDescription());
        project.setTechnologies(request.getTechnologies());
        project.setRoleContribution(request.getRoleContribution());
        project.setKeyFeatures(request.getKeyFeatures());
        project.setArchitectureDetails(request.getArchitectureDetails());
        if (request.getDifficulty() != null) {
            project.setDifficulty(request.getDifficulty());
        }

        Project updatedProject = projectRepository.save(project);
        return mapToProjectResponse(updatedProject);
    }

    @Transactional
    public ProjectResponse analyzeProject(String userEmail, Long projectId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId + " for current user"));

        String prompt = projectAnalysisPromptBuilder.buildProjectAnalysisPrompt(project);
        String rawGroqResponse = callGroqDirectly(prompt);

        project.setAiAnalysisJson(rawGroqResponse);
        Project savedProject = projectRepository.save(project);

        return mapToProjectResponse(savedProject);
    }

    @Transactional
    public void deleteProject(String userEmail, Long projectId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId + " for current user"));

        projectRepository.delete(project);
    }

    private String callGroqDirectly(String prompt) {
        try {
            return groqAIService.callGroqChatApi(prompt);
        } catch (Exception e) {
            logger.error("Direct call to Groq AI failed for project analysis: {}", e.getMessage());
            return "{}";
        }
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getResume() != null ? project.getResume().getId() : null,
                project.getTitle(),
                project.getDescription(),
                project.getTechnologies(),
                project.getRoleContribution(),
                project.getKeyFeatures(),
                project.getArchitectureDetails(),
                project.getDifficulty(),
                project.getAiAnalysisJson(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
