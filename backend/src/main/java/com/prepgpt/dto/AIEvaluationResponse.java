package com.prepgpt.dto;

import java.util.List;

public class AIEvaluationResponse {

    private Integer score; // 0-10
    private Integer correctness; // 0-10
    private Integer relevance; // 0-10
    private Integer clarity; // 0-10
    private Integer technicalAccuracy; // 0-10
    private List<String> strengths;
    private List<String> weaknesses;
    private String feedback;
    private String improvementSuggestion;

    public AIEvaluationResponse() {
    }

    public AIEvaluationResponse(Integer score, Integer correctness, Integer relevance, Integer clarity,
                                Integer technicalAccuracy, List<String> strengths, List<String> weaknesses,
                                String feedback, String improvementSuggestion) {
        this.score = score;
        this.correctness = correctness;
        this.relevance = relevance;
        this.clarity = clarity;
        this.technicalAccuracy = technicalAccuracy;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.feedback = feedback;
        this.improvementSuggestion = improvementSuggestion;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getCorrectness() {
        return correctness;
    }

    public void setCorrectness(Integer correctness) {
        this.correctness = correctness;
    }

    public Integer getRelevance() {
        return relevance;
    }

    public void setRelevance(Integer relevance) {
        this.relevance = relevance;
    }

    public Integer getClarity() {
        return clarity;
    }

    public void setClarity(Integer clarity) {
        this.clarity = clarity;
    }

    public Integer getTechnicalAccuracy() {
        return technicalAccuracy;
    }

    public void setTechnicalAccuracy(Integer technicalAccuracy) {
        this.technicalAccuracy = technicalAccuracy;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getImprovementSuggestion() {
        return improvementSuggestion;
    }

    public void setImprovementSuggestion(String improvementSuggestion) {
        this.improvementSuggestion = improvementSuggestion;
    }
}
