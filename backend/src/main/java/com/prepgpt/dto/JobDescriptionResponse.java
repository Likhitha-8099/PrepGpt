package com.prepgpt.dto;

import com.prepgpt.entity.JobDescription;
import java.time.LocalDateTime;

public class JobDescriptionResponse {

    private Long id;
    private String targetRole;
    private String companyName;
    private String rawText;
    private String extractedSkillsJson;
    private String experienceLevel;
    private String keyResponsibilitiesJson;
    private String aiAnalysisSummary;
    private LocalDateTime createdAt;

    public JobDescriptionResponse() {
    }

    public JobDescriptionResponse(JobDescription entity) {
        if (entity != null) {
            this.id = entity.getId();
            this.targetRole = entity.getTargetRole();
            this.companyName = entity.getCompanyName();
            this.rawText = entity.getRawText();
            this.extractedSkillsJson = entity.getExtractedSkillsJson();
            this.experienceLevel = entity.getExperienceLevel();
            this.keyResponsibilitiesJson = entity.getKeyResponsibilitiesJson();
            this.aiAnalysisSummary = entity.getAiAnalysisSummary();
            this.createdAt = entity.getCreatedAt();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getExtractedSkillsJson() {
        return extractedSkillsJson;
    }

    public void setExtractedSkillsJson(String extractedSkillsJson) {
        this.extractedSkillsJson = extractedSkillsJson;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getKeyResponsibilitiesJson() {
        return keyResponsibilitiesJson;
    }

    public void setKeyResponsibilitiesJson(String keyResponsibilitiesJson) {
        this.keyResponsibilitiesJson = keyResponsibilitiesJson;
    }

    public String getAiAnalysisSummary() {
        return aiAnalysisSummary;
    }

    public void setAiAnalysisSummary(String aiAnalysisSummary) {
        this.aiAnalysisSummary = aiAnalysisSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
