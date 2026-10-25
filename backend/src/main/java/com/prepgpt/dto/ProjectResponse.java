package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import java.time.LocalDateTime;

public class ProjectResponse {

    private Long id;
    private Long resumeId;
    private String title;
    private String description;
    private String technologies;
    private String roleContribution;
    private String keyFeatures;
    private String architectureDetails;
    private Difficulty difficulty;
    private String aiAnalysisJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProjectResponse() {
    }

    public ProjectResponse(Long id, Long resumeId, String title, String description,
                           String technologies, String roleContribution, String keyFeatures,
                           String architectureDetails, Difficulty difficulty, String aiAnalysisJson,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.resumeId = resumeId;
        this.title = title;
        this.description = description;
        this.technologies = technologies;
        this.roleContribution = roleContribution;
        this.keyFeatures = keyFeatures;
        this.architectureDetails = architectureDetails;
        this.difficulty = difficulty;
        this.aiAnalysisJson = aiAnalysisJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTechnologies() {
        return technologies;
    }

    public void setTechnologies(String technologies) {
        this.technologies = technologies;
    }

    public String getRoleContribution() {
        return roleContribution;
    }

    public void setRoleContribution(String roleContribution) {
        this.roleContribution = roleContribution;
    }

    public String getKeyFeatures() {
        return keyFeatures;
    }

    public void setKeyFeatures(String keyFeatures) {
        this.keyFeatures = keyFeatures;
    }

    public String getArchitectureDetails() {
        return architectureDetails;
    }

    public void setArchitectureDetails(String architectureDetails) {
        this.architectureDetails = architectureDetails;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getAiAnalysisJson() {
        return aiAnalysisJson;
    }

    public void setAiAnalysisJson(String aiAnalysisJson) {
        this.aiAnalysisJson = aiAnalysisJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
