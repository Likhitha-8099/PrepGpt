package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.InterviewInteractionMode;
import com.prepgpt.entity.InterviewMode;
import com.prepgpt.entity.SessionStatus;

import java.time.LocalDateTime;
import java.util.List;

public class InterviewReportResponse {

    private Long sessionId;
    private Long categoryId;
    private String categoryName;
    private Difficulty difficulty;
    private InterviewMode mode;
    private InterviewInteractionMode interactionMode;
    private SessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer durationMinutes;

    private String jdTargetRole;
    private String jdCompanyName;

    private Long projectId;
    private String projectTitle;

    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Double completionRatePercentage;

    private Double overallScore;
    private Double correctnessScore;
    private Double relevanceScore;
    private Double clarityScore;
    private Double technicalAccuracyScore;

    private String performanceLevel;
    private List<String> keyStrengths;
    private List<String> keyWeaknesses;

    private AdaptiveSummaryDto adaptiveSummary;
    private List<QuestionEvaluationReportDto> questions;

    public InterviewReportResponse() {
    }

    public InterviewReportResponse(Long sessionId, Long categoryId, String categoryName, Difficulty difficulty,
                                   InterviewMode mode, InterviewInteractionMode interactionMode, SessionStatus status,
                                   LocalDateTime startedAt, LocalDateTime completedAt, Integer durationMinutes,
                                   Integer totalQuestions, Integer answeredQuestions, Double completionRatePercentage,
                                   Double overallScore, Double correctnessScore, Double relevanceScore,
                                   Double clarityScore, Double technicalAccuracyScore, String performanceLevel,
                                   List<String> keyStrengths, List<String> keyWeaknesses,
                                   AdaptiveSummaryDto adaptiveSummary, List<QuestionEvaluationReportDto> questions) {
        this.sessionId = sessionId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.mode = mode;
        this.interactionMode = interactionMode;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationMinutes = durationMinutes;
        this.totalQuestions = totalQuestions;
        this.answeredQuestions = answeredQuestions;
        this.completionRatePercentage = completionRatePercentage;
        this.overallScore = overallScore;
        this.correctnessScore = correctnessScore;
        this.relevanceScore = relevanceScore;
        this.clarityScore = clarityScore;
        this.technicalAccuracyScore = technicalAccuracyScore;
        this.performanceLevel = performanceLevel;
        this.keyStrengths = keyStrengths;
        this.keyWeaknesses = keyWeaknesses;
        this.adaptiveSummary = adaptiveSummary;
        this.questions = questions;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public InterviewMode getMode() {
        return mode;
    }

    public void setMode(InterviewMode mode) {
        this.mode = mode;
    }

    public InterviewInteractionMode getInteractionMode() {
        return interactionMode;
    }

    public void setInteractionMode(InterviewInteractionMode interactionMode) {
        this.interactionMode = interactionMode;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(Integer answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public Double getCompletionRatePercentage() {
        return completionRatePercentage;
    }

    public void setCompletionRatePercentage(Double completionRatePercentage) {
        this.completionRatePercentage = completionRatePercentage;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public Double getCorrectnessScore() {
        return correctnessScore;
    }

    public void setCorrectnessScore(Double correctnessScore) {
        this.correctnessScore = correctnessScore;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Double getClarityScore() {
        return clarityScore;
    }

    public void setClarityScore(Double clarityScore) {
        this.clarityScore = clarityScore;
    }

    public Double getTechnicalAccuracyScore() {
        return technicalAccuracyScore;
    }

    public void setTechnicalAccuracyScore(Double technicalAccuracyScore) {
        this.technicalAccuracyScore = technicalAccuracyScore;
    }

    public String getPerformanceLevel() {
        return performanceLevel;
    }

    public void setPerformanceLevel(String performanceLevel) {
        this.performanceLevel = performanceLevel;
    }

    public List<String> getKeyStrengths() {
        return keyStrengths;
    }

    public void setKeyStrengths(List<String> keyStrengths) {
        this.keyStrengths = keyStrengths;
    }

    public List<String> getKeyWeaknesses() {
        return keyWeaknesses;
    }

    public void setKeyWeaknesses(List<String> keyWeaknesses) {
        this.keyWeaknesses = keyWeaknesses;
    }

    public AdaptiveSummaryDto getAdaptiveSummary() {
        return adaptiveSummary;
    }

    public void setAdaptiveSummary(AdaptiveSummaryDto adaptiveSummary) {
        this.adaptiveSummary = adaptiveSummary;
    }

    public List<QuestionEvaluationReportDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionEvaluationReportDto> questions) {
        this.questions = questions;
    }

    public String getJdTargetRole() {
        return jdTargetRole;
    }

    public void setJdTargetRole(String jdTargetRole) {
        this.jdTargetRole = jdTargetRole;
    }

    public String getJdCompanyName() {
        return jdCompanyName;
    }

    public void setJdCompanyName(String jdCompanyName) {
        this.jdCompanyName = jdCompanyName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }
}
