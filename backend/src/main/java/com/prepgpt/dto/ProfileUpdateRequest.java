package com.prepgpt.dto;

public class ProfileUpdateRequest {

    private String bio;
    private String targetRole;
    private String experienceLevel;
    private String phone;
    private String githubUrl;
    private String linkedinUrl;

    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String bio, String targetRole, String experienceLevel, String phone, String githubUrl, String linkedinUrl) {
        this.bio = bio;
        this.targetRole = targetRole;
        this.experienceLevel = experienceLevel;
        this.phone = phone;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
}
