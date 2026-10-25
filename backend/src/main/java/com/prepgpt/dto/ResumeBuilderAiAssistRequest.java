package com.prepgpt.dto;

import jakarta.validation.constraints.NotBlank;

public class ResumeBuilderAiAssistRequest {

    @NotBlank(message = "Action is required")
    private String action; // IMPROVE_SUMMARY, IMPROVE_BULLET, SUGGEST_SKILLS, JD_KEYWORD_MATCH, FORMAT_AUDIT

    private String summary;
    private String bullet;
    private String experienceContext;
    private String skillsContext;
    private String projectsContext;
    private String jobDescription;
    private String targetRole;

    public ResumeBuilderAiAssistRequest() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBullet() {
        return bullet;
    }

    public void setBullet(String bullet) {
        this.bullet = bullet;
    }

    public String getExperienceContext() {
        return experienceContext;
    }

    public void setExperienceContext(String experienceContext) {
        this.experienceContext = experienceContext;
    }

    public String getSkillsContext() {
        return skillsContext;
    }

    public void setSkillsContext(String skillsContext) {
        this.skillsContext = skillsContext;
    }

    public String getProjectsContext() {
        return projectsContext;
    }

    public void setProjectsContext(String projectsContext) {
        this.projectsContext = projectsContext;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
}
