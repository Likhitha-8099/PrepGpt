package com.prepgpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.ResumeBuilderPromptBuilder;
import com.prepgpt.dto.*;
import com.prepgpt.entity.Project;
import com.prepgpt.entity.Resume;
import com.prepgpt.entity.StudentProfile;
import com.prepgpt.entity.User;
import com.prepgpt.exception.BadRequestException;
import com.prepgpt.exception.ResourceNotFoundException;
import com.prepgpt.repository.ProjectRepository;
import com.prepgpt.repository.ResumeRepository;
import com.prepgpt.repository.StudentProfileRepository;
import com.prepgpt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeBuilderService.class);

    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ProjectRepository projectRepository;
    private final GroqAIService groqAIService;
    private final ResumeBuilderPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Path uploadDir;

    public ResumeBuilderService(
            UserRepository userRepository,
            StudentProfileRepository profileRepository,
            ResumeRepository resumeRepository,
            ProjectRepository projectRepository,
            GroqAIService groqAIService,
            ResumeBuilderPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            @Value("${app.upload.dir:uploads/resumes}") String uploadDirPath) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.resumeRepository = resumeRepository;
        this.projectRepository = projectRepository;
        this.groqAIService = groqAIService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public ResumeBuilderInitialDataResponse getInitialData(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ResumeBuilderInitialDataResponse response = new ResumeBuilderInitialDataResponse();
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());

        // Fetch Student Profile if available
        Optional<StudentProfile> profileOpt = profileRepository.findByUserId(user.getId());
        if (profileOpt.isPresent()) {
            StudentProfile profile = profileOpt.get();
            response.setPhone(profile.getPhone());
            response.setBio(profile.getBio());
            response.setTargetRole(profile.getTargetRole());
            response.setExperienceLevel(profile.getExperienceLevel());
            response.setGithubUrl(profile.getGithubUrl());
            response.setLinkedinUrl(profile.getLinkedinUrl());
        }

        // Fetch uploaded resumes
        List<Resume> resumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<ResumeResponse> resumeResponses = resumes.stream()
                .map(this::mapToResumeResponse)
                .collect(Collectors.toList());
        response.setUploadedResumes(resumeResponses);

        // Fetch candidate projects
        List<Project> projects = projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<ProjectResponse> projectResponses = projects.stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
        response.setProjects(projectResponses);

        return response;
    }

    @Transactional
    public ResumeBuilderAiAssistResponse processAiAssist(String userEmail, ResumeBuilderAiAssistRequest request) {
        // Verify user exists and is authenticated
        userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        String prompt = promptBuilder.buildPrompt(request);
        ResumeBuilderAiAssistResponse response = new ResumeBuilderAiAssistResponse();
        response.setAction(request.getAction());

        try {
            String rawJson = groqAIService.callGroqChatApi(prompt);
            JsonNode root = objectMapper.readTree(rawJson);

            if (root.has("improvedText")) {
                response.setImprovedText(root.path("improvedText").asText(""));
            }

            if (root.has("suggestions") && root.path("suggestions").isArray()) {
                List<String> suggestions = new ArrayList<>();
                for (JsonNode sNode : root.path("suggestions")) {
                    suggestions.add(sNode.asText());
                }
                response.setSuggestions(suggestions);
            }

            if (root.has("matchedKeywords") && root.path("matchedKeywords").isArray()) {
                List<String> matched = new ArrayList<>();
                for (JsonNode kNode : root.path("matchedKeywords")) {
                    matched.add(kNode.asText());
                }
                response.setMatchedKeywords(matched);
            }

            if (root.has("missingKeywords") && root.path("missingKeywords").isArray()) {
                List<String> missing = new ArrayList<>();
                for (JsonNode kNode : root.path("missingKeywords")) {
                    missing.add(kNode.asText());
                }
                response.setMissingKeywords(missing);
            }

            if (root.has("auditTips") && root.path("auditTips").isArray()) {
                List<String> auditTips = new ArrayList<>();
                for (JsonNode tNode : root.path("auditTips")) {
                    auditTips.add(tNode.asText());
                }
                response.setAuditTips(auditTips);
            }

            if (root.has("atsReadinessScore")) {
                response.setAtsReadinessScore(root.path("atsReadinessScore").asInt(85));
            } else {
                response.setAtsReadinessScore(85);
            }

        } catch (Exception e) {
            logger.warn("Groq AI Resume assistance error, falling back gracefully: {}", e.getMessage());
            fallbackResponse(request, response);
        }

        return response;
    }

    @Transactional
    public ResumeResponse saveResumeFromBuilder(String userEmail, ResumeBuilderSaveRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (request.getPlainTextContent() == null || request.getPlainTextContent().isBlank()) {
            throw new BadRequestException("Resume text content cannot be blank.");
        }

        String templateSuffix = request.getTemplateId() != null && !request.getTemplateId().isBlank()
                ? "_" + request.getTemplateId().trim().toLowerCase().replaceAll("[^a-z0-9_-]", "")
                : "";
        String cleanFileName = request.getFileName() != null && !request.getFileName().isBlank()
                ? request.getFileName().trim()
                : "ATS_Resume" + templateSuffix + ".txt";

        // Save plain text representation to uploads directory
        String storedFileName = "builder_" + UUID.randomUUID().toString() + ".txt";
        Path targetPath = uploadDir.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(uploadDir);
            Files.writeString(targetPath, request.getPlainTextContent(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Could not write builder resume file to disk: {}", e.getMessage());
        }

        long fileSize = request.getPlainTextContent().getBytes(StandardCharsets.UTF_8).length;

        Resume resume = new Resume(
                user,
                cleanFileName,
                targetPath.toString(),
                "TXT",
                fileSize,
                request.getPlainTextContent()
        );

        resume.setSkillsJson(request.getSkillsJson() != null ? request.getSkillsJson() : "[]");
        resume.setExperienceJson(request.getExperienceJson() != null ? request.getExperienceJson() : "[]");
        resume.setEducationJson(request.getEducationJson() != null ? request.getEducationJson() : "[]");
        resume.setCertificationsJson(request.getCertificationsJson() != null ? request.getCertificationsJson() : "[]");

        // If candidate doesn't have an existing primary resume, make this one primary
        List<Resume> existing = resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (existing.isEmpty()) {
            resume.setIsPrimary(true);
        } else {
            resume.setIsPrimary(false);
        }

        Resume saved = resumeRepository.save(resume);
        logger.info("Saved ATS Resume builder draft for user {} as resume id #{}", user.getEmail(), saved.getId());

        return mapToResumeResponse(saved);
    }

    private void fallbackResponse(ResumeBuilderAiAssistRequest request, ResumeBuilderAiAssistResponse response) {
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "IMPROVE_SUMMARY";
        switch (action) {
            case "IMPROVE_SUMMARY" -> {
                String original = request.getSummary() != null ? request.getSummary() : "Software Engineer passionate about high performance systems.";
                response.setImprovedText(original.trim() + " Demonstrates proven technical competency in scalable software engineering and cross-functional team delivery.");
                response.setSuggestions(List.of(
                        "Results-driven engineer specializing in clean code and reliable backend services.",
                        "Versatile software developer with end-to-end full-stack problem-solving experience."
                ));
            }
            case "IMPROVE_BULLET" -> {
                String bullet = request.getBullet() != null && !request.getBullet().isBlank()
                        ? request.getBullet().trim()
                        : "core system components";
                response.setImprovedText("Engineered and delivered " + bullet + ", improving operational throughput and code maintainability.");
                response.setSuggestions(List.of(
                        "Architected scalable modules and resolved technical bottlenecks across the service stack.",
                        "Collaborated with cross-functional teams to deploy robust features with high test coverage."
                ));
            }
            case "SUGGEST_SKILLS" -> {
                response.setImprovedText("Recommended standard technical competencies based on software engineering best practices.");
                response.setSuggestions(List.of("Git / Version Control", "RESTful Web APIs", "Agile / Scrum", "Unit Testing", "CI/CD Pipeline"));
            }
            case "JD_KEYWORD_MATCH" -> {
                response.setAtsReadinessScore(80);
                response.setImprovedText("Preliminary keyword analysis: standard software engineering terms aligned.");
                response.setMatchedKeywords(List.of("Software Engineering", "Development", "Problem Solving"));
                response.setMissingKeywords(List.of("Specific Cloud Vendor (AWS/GCP)", "Containerization"));
                response.setAuditTips(List.of(
                        "Include relevant certifications or coursework aligned with the job description.",
                        "Quantify impact with measurable milestones where appropriate."
                ));
            }
            default -> {
                response.setAtsReadinessScore(85);
                response.setAuditTips(List.of(
                        "Follow standard single-column layout.",
                        "Use clear section headers (Summary, Experience, Education, Skills).",
                        "Avoid graphics and tables that may confuse text extractors."
                ));
            }
        }
    }

    private ResumeResponse mapToResumeResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getFileType(),
                resume.getFileSize(),
                resume.getExtractedText(),
                resume.getSkillsJson(),
                resume.getExperienceJson(),
                resume.getEducationJson(),
                resume.getCertificationsJson(),
                resume.getRawAiAnalysis(),
                resume.getIsPrimary(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
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
