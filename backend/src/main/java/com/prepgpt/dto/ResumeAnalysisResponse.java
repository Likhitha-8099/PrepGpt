package com.prepgpt.dto;

import java.util.List;
import java.util.Map;

public class ResumeAnalysisResponse {

    private String summary;
    private List<String> skills;
    private List<Map<String, Object>> experience;
    private List<Map<String, Object>> education;
    private List<Map<String, Object>> projects;
    private List<String> certifications;

    public ResumeAnalysisResponse() {
    }

    public ResumeAnalysisResponse(String summary, List<String> skills,
                                  List<Map<String, Object>> experience,
                                  List<Map<String, Object>> education,
                                  List<Map<String, Object>> projects,
                                  List<String> certifications) {
        this.summary = summary;
        this.skills = skills;
        this.experience = experience;
        this.education = education;
        this.projects = projects;
        this.certifications = certifications;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<Map<String, Object>> getExperience() {
        return experience;
    }

    public void setExperience(List<Map<String, Object>> experience) {
        this.experience = experience;
    }

    public List<Map<String, Object>> getEducation() {
        return education;
    }

    public void setEducation(List<Map<String, Object>> education) {
        this.education = education;
    }

    public List<Map<String, Object>> getProjects() {
        return projects;
    }

    public void setProjects(List<Map<String, Object>> projects) {
        this.projects = projects;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }
}
