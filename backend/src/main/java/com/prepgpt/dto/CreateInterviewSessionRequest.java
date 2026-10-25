package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateInterviewSessionRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    @NotNull(message = "Question count is required")
    @Min(value = 1, message = "Question count must be at least 1")
    @Max(value = 50, message = "Question count cannot exceed 50")
    private Integer questionCount;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 5, message = "Duration must be at least 5 minutes")
    @Max(value = 180, message = "Duration cannot exceed 180 minutes")
    private Integer durationMinutes;

    private com.prepgpt.entity.InterviewMode mode = com.prepgpt.entity.InterviewMode.QUESTION_BANK;
    private com.prepgpt.entity.InterviewInteractionMode interactionMode = com.prepgpt.entity.InterviewInteractionMode.TEXT;
    private Long resumeId;
    private Long projectId;
    private Long jobDescriptionId;

    public CreateInterviewSessionRequest() {
    }

    public CreateInterviewSessionRequest(Long categoryId, Difficulty difficulty, Integer questionCount, Integer durationMinutes) {
        this(categoryId, difficulty, questionCount, durationMinutes, com.prepgpt.entity.InterviewMode.QUESTION_BANK, com.prepgpt.entity.InterviewInteractionMode.TEXT);
    }

    public CreateInterviewSessionRequest(Long categoryId, Difficulty difficulty, Integer questionCount, Integer durationMinutes, com.prepgpt.entity.InterviewMode mode) {
        this(categoryId, difficulty, questionCount, durationMinutes, mode, com.prepgpt.entity.InterviewInteractionMode.TEXT);
    }

    public CreateInterviewSessionRequest(Long categoryId, Difficulty difficulty, Integer questionCount, Integer durationMinutes, com.prepgpt.entity.InterviewMode mode, com.prepgpt.entity.InterviewInteractionMode interactionMode) {
        this.categoryId = categoryId;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.durationMinutes = durationMinutes;
        this.mode = mode != null ? mode : com.prepgpt.entity.InterviewMode.QUESTION_BANK;
        this.interactionMode = interactionMode != null ? interactionMode : com.prepgpt.entity.InterviewInteractionMode.TEXT;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
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

    public Long getJobDescriptionId() {
        return jobDescriptionId;
    }

    public void setJobDescriptionId(Long jobDescriptionId) {
        this.jobDescriptionId = jobDescriptionId;
    }
}
