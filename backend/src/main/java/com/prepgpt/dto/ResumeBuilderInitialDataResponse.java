package com.prepgpt.dto;

import java.util.ArrayList;
import java.util.List;

public class ResumeBuilderInitialDataResponse {

    private String fullName;
    private String email;
    private String phone;
    private String bio;
    private String targetRole;
    private String experienceLevel;
    private String githubUrl;
    private String linkedinUrl;

    private List<ResumeResponse> uploadedResumes = new ArrayList<>();
    private List<ProjectResponse> projects = new ArrayList<>();

    public ResumeBuilderInitialDataResponse() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public List<ResumeResponse> getUploadedResumes() {
        return uploadedResumes;
    }

    public void setUploadedResumes(List<ResumeResponse> uploadedResumes) {
        this.uploadedResumes = uploadedResumes;
    }

    public List<ProjectResponse> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectResponse> projects) {
        this.projects = projects;
    }
}
