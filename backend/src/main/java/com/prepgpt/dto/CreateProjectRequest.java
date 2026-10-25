package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;

public class CreateProjectRequest {

    @NotBlank(message = "Project title is required")
    private String title;

    private String description;
    private String technologies;
    private String roleContribution;
    private String keyFeatures;
    private String architectureDetails;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private Long resumeId;

    public CreateProjectRequest() {
    }

    public CreateProjectRequest(String title, String description, String technologies, String roleContribution) {
        this.title = title;
        this.description = description;
        this.technologies = technologies;
        this.roleContribution = roleContribution;
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

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }
}
