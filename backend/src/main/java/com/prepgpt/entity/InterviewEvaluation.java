package com.prepgpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_evaluations")
public class InterviewEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "interview_response_id", nullable = false, unique = true)
    private InterviewResponse interviewResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EvaluationStatus status = EvaluationStatus.COMPLETED;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at", nullable = true)
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT", nullable = true)
    private String lastError;

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(nullable = true)
    private Integer score;

    @Column(nullable = true)
    private Integer correctness;

    @Column(nullable = true)
    private Integer relevance;

    @Column(nullable = true)
    private Integer clarity;

    @Column(name = "technical_accuracy", nullable = true)
    private Integer technicalAccuracy;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "improvement_suggestion", columnDefinition = "TEXT")
    private String improvementSuggestion;

    @Column(name = "should_ask_follow_up", nullable = false)
    private Boolean shouldAskFollowUp = false;

    @Column(name = "follow_up_question", columnDefinition = "TEXT")
    private String followUpQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "adaptive_decision", nullable = true)
    private AdaptiveDecision adaptiveDecision;

    @Column(name = "adaptive_reason", columnDefinition = "TEXT", nullable = true)
    private String adaptiveReason;

    @Column(name = "target_concept", nullable = true)
    private String targetConcept;

    @Column(name = "user_facing_message", columnDefinition = "TEXT", nullable = true)
    private String userFacingMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_difficulty", nullable = true)
    private Difficulty nextDifficulty;

    @Column(name = "evaluated_at", nullable = true)
    private LocalDateTime evaluatedAt;

    public InterviewEvaluation() {
    }

    public InterviewEvaluation(InterviewResponse interviewResponse, Integer score, Integer correctness,
                               Integer relevance, Integer clarity, Integer technicalAccuracy,
                               String strengths, String weaknesses, String feedback,
                               String improvementSuggestion, Boolean shouldAskFollowUp,
                               String followUpQuestion) {
        this.interviewResponse = interviewResponse;
        this.score = score;
        this.correctness = correctness;
        this.relevance = relevance;
        this.clarity = clarity;
        this.technicalAccuracy = technicalAccuracy;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.feedback = feedback;
        this.improvementSuggestion = improvementSuggestion;
        this.shouldAskFollowUp = shouldAskFollowUp != null ? shouldAskFollowUp : false;
        this.followUpQuestion = followUpQuestion;
    }

    @PrePersist
    protected void onCreate() {
        this.evaluatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewResponse getInterviewResponse() {
        return interviewResponse;
    }

    public void setInterviewResponse(InterviewResponse interviewResponse) {
        this.interviewResponse = interviewResponse;
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

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
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

    public AdaptiveDecision getAdaptiveDecision() {
        return adaptiveDecision;
    }

    public void setAdaptiveDecision(AdaptiveDecision adaptiveDecision) {
        this.adaptiveDecision = adaptiveDecision;
    }

    public String getAdaptiveReason() {
        return adaptiveReason;
    }

    public void setAdaptiveReason(String adaptiveReason) {
        this.adaptiveReason = adaptiveReason;
    }

    public String getTargetConcept() {
        return targetConcept;
    }

    public void setTargetConcept(String targetConcept) {
        this.targetConcept = targetConcept;
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

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
