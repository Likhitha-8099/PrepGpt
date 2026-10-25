package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.InterviewMode;
import com.prepgpt.entity.SessionStatus;

import java.time.LocalDateTime;

public class AdminSessionSummaryResponse {

    private Long sessionId;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String categoryName;
    private InterviewMode mode;
    private Difficulty difficulty;
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer questionCount;
    private Double averageScore;

    public AdminSessionSummaryResponse() {}

    public AdminSessionSummaryResponse(Long sessionId, Long userId, String userEmail,
                                       String userFullName, String categoryName,
                                       InterviewMode mode, Difficulty difficulty,
                                       SessionStatus status, LocalDateTime createdAt,
                                       LocalDateTime completedAt, Integer questionCount,
                                       Double averageScore) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userFullName = userFullName;
        this.categoryName = categoryName;
        this.mode = mode;
        this.difficulty = difficulty;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.questionCount = questionCount;
        this.averageScore = averageScore;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public InterviewMode getMode() { return mode; }
    public void setMode(InterviewMode mode) { this.mode = mode; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
}
