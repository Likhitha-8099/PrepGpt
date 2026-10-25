package com.prepgpt.dto;

import com.prepgpt.entity.AdaptiveDecision;
import com.prepgpt.entity.Difficulty;

import java.time.LocalDateTime;
import java.util.List;

public class EvaluationResponse {

    private Integer score;
    private Integer correctness;
    private Integer relevance;
    private Integer clarity;
    private Integer technicalAccuracy;
    private List<String> strengths;
    private List<String> weaknesses;
    private String feedback;
    private String improvementSuggestion;
    private Boolean shouldAskFollowUp;
    private String followUpQuestion;
    private LocalDateTime evaluatedAt;

    private AdaptiveDecision adaptiveDecision;
    private String userFacingMessage;
    private Difficulty nextDifficulty;
    private String targetConcept;

    public EvaluationResponse() {
    }

    public EvaluationResponse(Integer score, Integer correctness, Integer relevance, Integer clarity,
                               Integer technicalAccuracy, List<String> strengths, List<String> weaknesses,
                               String feedback, String improvementSuggestion, Boolean shouldAskFollowUp,
                               String followUpQuestion, LocalDateTime evaluatedAt) {
        this.score = score;
        this.correctness = correctness;
        this.relevance = relevance;
        this.clarity = clarity;
        this.technicalAccuracy = technicalAccuracy;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.feedback = feedback;
        this.improvementSuggestion = improvementSuggestion;
        this.shouldAskFollowUp = shouldAskFollowUp;
        this.followUpQuestion = followUpQuestion;
        this.evaluatedAt = evaluatedAt;
    }

    public EvaluationResponse(Integer score, Integer correctness, Integer relevance, Integer clarity,
                               Integer technicalAccuracy, List<String> strengths, List<String> weaknesses,
                               String feedback, String improvementSuggestion, Boolean shouldAskFollowUp,
                               String followUpQuestion, LocalDateTime evaluatedAt,
                               AdaptiveDecision adaptiveDecision, String userFacingMessage,
                               Difficulty nextDifficulty, String targetConcept) {
        this.score = score;
        this.correctness = correctness;
        this.relevance = relevance;
        this.clarity = clarity;
        this.technicalAccuracy = technicalAccuracy;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.feedback = feedback;
        this.improvementSuggestion = improvementSuggestion;
        this.shouldAskFollowUp = shouldAskFollowUp;
        this.followUpQuestion = followUpQuestion;
        this.evaluatedAt = evaluatedAt;
        this.adaptiveDecision = adaptiveDecision;
        this.userFacingMessage = userFacingMessage;
        this.nextDifficulty = nextDifficulty;
        this.targetConcept = targetConcept;
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

    public Boolean getShouldAskFollowUp() {
        return shouldAskFollowUp;
    }

    public void setShouldAskFollowUp(Boolean shouldAskFollowUp) {
        this.shouldAskFollowUp = shouldAskFollowUp;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public void setFollowUpQuestion(String followUpQuestion) {
        this.followUpQuestion = followUpQuestion;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public AdaptiveDecision getAdaptiveDecision() {
        return adaptiveDecision;
    }

    public void setAdaptiveDecision(AdaptiveDecision adaptiveDecision) {
        this.adaptiveDecision = adaptiveDecision;
    }

    public String getUserFacingMessage() {
        return userFacingMessage;
    }

    public void setUserFacingMessage(String userFacingMessage) {
        this.userFacingMessage = userFacingMessage;
    }

    public Difficulty getNextDifficulty() {
        return nextDifficulty;
    }

    public void setNextDifficulty(Difficulty nextDifficulty) {
        this.nextDifficulty = nextDifficulty;
    }

    public String getTargetConcept() {
        return targetConcept;
    }

    public void setTargetConcept(String targetConcept) {
        this.targetConcept = targetConcept;
    }
}
