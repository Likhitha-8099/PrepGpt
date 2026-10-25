package com.prepgpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.ai.GroqAIService;
import com.prepgpt.ai.prompt.JdAnalysisPromptBuilder;
import com.prepgpt.dto.CreateJobDescriptionRequest;
import com.prepgpt.entity.InterviewSession;
import com.prepgpt.entity.JobDescription;
import com.prepgpt.entity.User;
import com.prepgpt.repository.InterviewSessionRepository;
import com.prepgpt.repository.JobDescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobDescriptionAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(JobDescriptionAnalysisService.class);

    private static final String[] COMMON_TECH_KEYWORDS = {
        "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Rust", "PHP", "Ruby",
        "Spring", "Spring Boot", "React", "Angular", "Vue", "Node.js", "Express", "Django", "Flask",
        "SQL", "MySQL", "PostgreSQL", "MongoDB", "Redis", "Kafka", "Docker", "Kubernetes", "AWS",
        "Azure", "GCP", "Microservices", "REST", "GraphQL", "Git", "CI/CD", "Linux"
    };

    private final JobDescriptionRepository jobDescriptionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final GroqAIService groqAIService;
    private final JdAnalysisPromptBuilder jdAnalysisPromptBuilder;
    private final ObjectMapper objectMapper;

    public JobDescriptionAnalysisService(JobDescriptionRepository jobDescriptionRepository,
                                         InterviewSessionRepository sessionRepository,
                                         GroqAIService groqAIService,
                                         JdAnalysisPromptBuilder jdAnalysisPromptBuilder,
                                         ObjectMapper objectMapper) {
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.sessionRepository = sessionRepository;
        this.groqAIService = groqAIService;
        this.jdAnalysisPromptBuilder = jdAnalysisPromptBuilder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JobDescription analyzeAndSaveJobDescription(User user, CreateJobDescriptionRequest request) {
        if (request == null || request.getRawText() == null || request.getRawText().trim().isEmpty()) {
            throw new IllegalArgumentException("Job description text cannot be empty");
        }

        String rawText = request.getRawText().trim();
        if (rawText.length() > 10000) {
            throw new IllegalArgumentException("Job description exceeds maximum allowed length of 10,000 characters");
        }

        // 1. Deterministic skill scanning
        Set<String> deterministicSkills = scanDeterministicSkills(rawText);

        String targetRole = request.getTargetRole() != null && !request.getTargetRole().trim().isEmpty()
                ? request.getTargetRole().trim() : null;
        String companyName = request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()
                ? request.getCompanyName().trim() : null;
        String experienceLevel = "MID";
        List<String> extractedSkills = new ArrayList<>(deterministicSkills);
        List<String> keyResponsibilities = new ArrayList<>();
        String summary = "Job description requirements analyzed.";

        try {
            String prompt = jdAnalysisPromptBuilder.buildAnalysisPrompt(rawText, request.getTargetRole(), request.getCompanyName());
            String aiResponse = groqAIService.callGroqChatApi(prompt);
            String jsonContent = extractJsonBlock(aiResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            if (targetRole == null && root.hasNonNull("targetRole")) {
                targetRole = root.get("targetRole").asText().trim();
            }
            if (companyName == null && root.hasNonNull("companyName")) {
                companyName = root.get("companyName").asText().trim();
            }
            if (root.hasNonNull("experienceLevel")) {
                experienceLevel = root.get("experienceLevel").asText().trim();
            }
            if (root.has("skills") && root.get("skills").isArray()) {
                for (JsonNode node : root.get("skills")) {
                    String skill = node.asText().trim();
                    if (!skill.isEmpty() && !extractedSkills.contains(skill)) {
                        extractedSkills.add(skill);
                    }
                }
            }
            if (root.has("keyResponsibilities") && root.get("keyResponsibilities").isArray()) {
                for (JsonNode node : root.get("keyResponsibilities")) {
                    String resp = node.asText().trim();
                    if (!resp.isEmpty() && !keyResponsibilities.contains(resp)) {
                        keyResponsibilities.add(resp);
                    }
                }
            }
            if (root.hasNonNull("summary")) {
                summary = root.get("summary").asText().trim();
            }

        } catch (Exception e) {
            logger.warn("Failed to parse AI JSON output for JD analysis, using deterministic fallback: {}", e.getMessage());
        }

        if (targetRole == null || targetRole.isEmpty()) {
            targetRole = "Software Engineer";
        }

        JobDescription jd = new JobDescription();
        jd.setUser(user);
        jd.setTargetRole(targetRole);
        jd.setCompanyName(companyName);
        jd.setRawText(rawText);
        jd.setExperienceLevel(experienceLevel);

        try {
            jd.setExtractedSkillsJson(objectMapper.writeValueAsString(extractedSkills));
            jd.setKeyResponsibilitiesJson(objectMapper.writeValueAsString(keyResponsibilities));
        } catch (Exception e) {
            jd.setExtractedSkillsJson("[]");
            jd.setKeyResponsibilitiesJson("[]");
        }

        jd.setAiAnalysisSummary(summary);

        return jobDescriptionRepository.save(jd);
    }

    public JobDescription getJobDescriptionForUser(Long id, Long userId) {
        return jobDescriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Job description not found or access denied"));
    }

    public List<JobDescription> getAllJobDescriptionsForUser(Long userId) {
        return jobDescriptionRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void deleteJobDescriptionForUser(Long id, Long userId) {
        JobDescription jd = getJobDescriptionForUser(id, userId);
        List<InterviewSession> sessions = sessionRepository.findByJobDescriptionId(jd.getId());
        for (InterviewSession session : sessions) {
            session.setJobDescription(null);
            sessionRepository.save(session);
        }
        jobDescriptionRepository.delete(jd);
    }

    private Set<String> scanDeterministicSkills(String text) {
        Set<String> found = new LinkedHashSet<>();
        for (String kw : COMMON_TECH_KEYWORDS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                found.add(kw);
            }
        }
        return found;
    }

    private String extractJsonBlock(String text) {
        if (text == null) return "{}";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }
}
