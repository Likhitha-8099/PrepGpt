package com.prepgpt.dto;

public class StudentProfileResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String bio;
    private String targetRole;
    private String experienceLevel;
    private String phone;
    private String githubUrl;
    private String linkedinUrl;

    public StudentProfileResponse() {
    }

    public StudentProfileResponse(Long id, Long userId, String fullName, String email, String role,
                                 String bio, String targetRole, String experienceLevel,
                                 String phone, String githubUrl, String linkedinUrl) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.bio = bio;
        this.targetRole = targetRole;
        this.experienceLevel = experienceLevel;
        this.phone = phone;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
