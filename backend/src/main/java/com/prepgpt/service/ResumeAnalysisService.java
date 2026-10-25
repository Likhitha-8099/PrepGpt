package com.prepgpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.ResumeAnalysisPromptBuilder;
import com.prepgpt.dto.ResumeResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeAnalysisService.class);

    private final ResumeRepository resumeRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ResumeParserService resumeParserService;
    private final ResumeAnalysisPromptBuilder resumeAnalysisPromptBuilder;
    private final GroqAIService groqAIService;
    private final ObjectMapper objectMapper;

    public ResumeAnalysisService(ResumeRepository resumeRepository,
                                 ProjectRepository projectRepository,
                                 UserRepository userRepository,
                                 ResumeParserService resumeParserService,
                                 ResumeAnalysisPromptBuilder resumeAnalysisPromptBuilder,
                                 GroqAIService groqAIService,
                                 ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.resumeParserService = resumeParserService;
        this.resumeAnalysisPromptBuilder = resumeAnalysisPromptBuilder;
        this.groqAIService = groqAIService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ResumeResponse uploadAndAnalyzeResume(String userEmail, MultipartFile file) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        // Step 1: Disk Storage & Text Extraction via Apache Tika
        ResumeParserService.FileStorageResult storageResult = resumeParserService.storeAndExtractText(file);

        // Step 2: Create base Resume entity
        Resume resume = new Resume(
                user,
                storageResult.getOriginalFileName(),
                storageResult.getStoredFilePath(),
                storageResult.getFileType(),
                storageResult.getFileSize(),
                storageResult.getExtractedText()
        );

        // Check if candidate has existing resumes. If none, set as primary
        List<Resume> existingResumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (existingResumes.isEmpty()) {
            resume.setIsPrimary(true);
        }

        Resume savedResume = resumeRepository.save(resume);

        // Step 3: Trigger Groq AI Resume Analysis
        try {
            analyzeResumeWithAI(savedResume);
        } catch (Exception e) {
            logger.warn("AI resume analysis failed for resume #{}, but extracted text is safely persisted: {}", savedResume.getId(), e.getMessage());
        }

        return mapToResumeResponse(savedResume);
    }

    @Transactional
    public ResumeResponse analyzeResume(String userEmail, Long resumeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id " + resumeId + " for current user"));

        analyzeResumeWithAI(resume);
        return mapToResumeResponse(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> getUserResumes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResumeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(String userEmail, Long resumeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id " + resumeId + " for current user"));

        return mapToResumeResponse(resume);
    }

    @Transactional
    public ResumeResponse setPrimaryResume(String userEmail, Long resumeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<Resume> userResumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        Resume targetResume = null;

        for (Resume r : userResumes) {
            if (r.getId().equals(resumeId)) {
                r.setIsPrimary(true);
                targetResume = r;
            } else {
                r.setIsPrimary(false);
            }
        }

        if (targetResume == null) {
            throw new ResourceNotFoundException("Resume not found with id " + resumeId + " for current user");
        }

        resumeRepository.saveAll(userResumes);
        return mapToResumeResponse(targetResume);
    }

    @Transactional
    public void deleteResume(String userEmail, Long resumeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id " + resumeId + " for current user"));

        // Delete underlying local file
        resumeParserService.deleteFile(resume.getStoredFilePath());

        // Delete DB record
        resumeRepository.delete(resume);
    }

    private void analyzeResumeWithAI(Resume resume) {
        String prompt = resumeAnalysisPromptBuilder.buildResumeAnalysisPrompt(resume.getExtractedText());
        String rawGroqResponse = callGroqDirectly(prompt);

        try {
            JsonNode root = objectMapper.readTree(rawGroqResponse);

            JsonNode skillsNode = root.path("skills");
            JsonNode expNode = root.path("experience");
            JsonNode eduNode = root.path("education");
            JsonNode projectsNode = root.path("projects");
            JsonNode certsNode = root.path("certifications");

            resume.setSkillsJson(objectMapper.writeValueAsString(skillsNode));
            resume.setExperienceJson(objectMapper.writeValueAsString(expNode));
            resume.setEducationJson(objectMapper.writeValueAsString(eduNode));
            resume.setCertificationsJson(objectMapper.writeValueAsString(certsNode));
            resume.setRawAiAnalysis(rawGroqResponse);

            resumeRepository.save(resume);

            // Automatically extract & save candidate projects mentioned in resume if not already existing
            if (projectsNode.isArray()) {
                for (JsonNode pNode : projectsNode) {
                    String title = pNode.path("title").asText(null);
                    if (title != null && !title.isBlank()) {
                        String desc = pNode.path("description").asText("");
                        String tech = pNode.path("technologies").asText("");
                        String contrib = pNode.path("roleContribution").asText("");

                        // Avoid duplicate project title for same user
                        List<Project> existingProjects = projectRepository.findByUserIdOrderByCreatedAtDesc(resume.getUser().getId());
                        boolean exists = existingProjects.stream().anyMatch(p -> p.getTitle().equalsIgnoreCase(title.trim()));
                        if (!exists) {
                            Project project = new Project(resume.getUser(), title.trim(), desc, tech, contrib);
                            project.setResume(resume);
                            projectRepository.save(project);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Failed to parse Groq AI resume analysis response: {}", rawGroqResponse, e);
        }
    }

    private String callGroqDirectly(String prompt) {
        try {
            return groqAIService.callGroqChatApi(prompt);
        } catch (Exception e) {
            logger.error("Direct call to Groq AI failed: {}", e.getMessage());
            return "{}";
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
}
