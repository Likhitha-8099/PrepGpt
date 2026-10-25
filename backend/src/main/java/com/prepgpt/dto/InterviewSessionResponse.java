package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.SessionStatus;

import java.time.LocalDateTime;

public class InterviewSessionResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Difficulty difficulty;
    private SessionStatus status;
    private com.prepgpt.entity.InterviewMode mode;
    private com.prepgpt.entity.InterviewInteractionMode interactionMode;
    private Integer questionCount;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Long resumeId;
    private Long projectId;
    private String resumeFileName;
    private String projectTitle;

    private Long jobDescriptionId;
    private String jdTargetRole;
    private String jdCompanyName;

    private Integer progress;
    private java.util.List<InterviewQuestionDetailResponse> questions;

    public InterviewSessionResponse() {
    }

    public InterviewSessionResponse(Long id, Long categoryId, String categoryName, Difficulty difficulty,
                                    SessionStatus status, Integer questionCount, Integer durationMinutes,
                                    LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime completedAt) {
        this(id, categoryId, categoryName, difficulty, status, com.prepgpt.entity.InterviewMode.QUESTION_BANK,
             com.prepgpt.entity.InterviewInteractionMode.TEXT, questionCount, durationMinutes, createdAt, startedAt, completedAt);
    }

    public InterviewSessionResponse(Long id, Long categoryId, String categoryName, Difficulty difficulty,
                                    SessionStatus status, com.prepgpt.entity.InterviewMode mode,
                                    Integer questionCount, Integer durationMinutes,
                                    LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime completedAt) {
        this(id, categoryId, categoryName, difficulty, status, mode, com.prepgpt.entity.InterviewInteractionMode.TEXT,
             questionCount, durationMinutes, createdAt, startedAt, completedAt);
    }

    public InterviewSessionResponse(Long id, Long categoryId, String categoryName, Difficulty difficulty,
                                    SessionStatus status, com.prepgpt.entity.InterviewMode mode,
                                    com.prepgpt.entity.InterviewInteractionMode interactionMode,
                                    Integer questionCount, Integer durationMinutes,
                                    LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime completedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.status = status;
        this.mode = mode != null ? mode : com.prepgpt.entity.InterviewMode.QUESTION_BANK;
        this.interactionMode = interactionMode != null ? interactionMode : com.prepgpt.entity.InterviewInteractionMode.TEXT;
        this.questionCount = questionCount;
        this.durationMinutes = durationMinutes;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public com.prepgpt.entity.InterviewMode getMode() {
        return mode;
    }

    public void setMode(com.prepgpt.entity.InterviewMode mode) {
        this.mode = mode;
    }

    public com.prepgpt.entity.InterviewInteractionMode getInteractionMode() {
        return interactionMode;
    }

    public void setInteractionMode(com.prepgpt.entity.InterviewInteractionMode interactionMode) {
        this.interactionMode = interactionMode;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public java.util.List<InterviewQuestionDetailResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(java.util.List<InterviewQuestionDetailResponse> questions) {
        this.questions = questions;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public Long getJobDescriptionId() {
        return jobDescriptionId;
    }

    public void setJobDescriptionId(Long jobDescriptionId) {
        this.jobDescriptionId = jobDescriptionId;
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
}
