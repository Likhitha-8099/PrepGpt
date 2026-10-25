package com.prepgpt.dto;

import java.util.List;

public class ProjectAnalysisResponse {

    private String projectTitle;
    private String technicalSummary;
    private List<String> architectureTopics;
    private List<String> keyChallenges;
    private List<String> suggestedInterviewQuestions;

    public ProjectAnalysisResponse() {
    }

    public ProjectAnalysisResponse(String projectTitle, String technicalSummary,
                                   List<String> architectureTopics, List<String> keyChallenges,
                                   List<String> suggestedInterviewQuestions) {
        this.projectTitle = projectTitle;
        this.technicalSummary = technicalSummary;
        this.architectureTopics = architectureTopics;
        this.keyChallenges = keyChallenges;
        this.suggestedInterviewQuestions = suggestedInterviewQuestions;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getTechnicalSummary() {
        return technicalSummary;
    }

    public void setTechnicalSummary(String technicalSummary) {
        this.technicalSummary = technicalSummary;
    }

    public List<String> getArchitectureTopics() {
        return architectureTopics;
    }

    public void setArchitectureTopics(List<String> architectureTopics) {
        this.architectureTopics = architectureTopics;
    }

    public List<String> getKeyChallenges() {
        return keyChallenges;
    }

    public void setKeyChallenges(List<String> keyChallenges) {
        this.keyChallenges = keyChallenges;
    }

    public List<String> getSuggestedInterviewQuestions() {
        return suggestedInterviewQuestions;
    }

    public void setSuggestedInterviewQuestions(List<String> suggestedInterviewQuestions) {
        this.suggestedInterviewQuestions = suggestedInterviewQuestions;
    }
}
