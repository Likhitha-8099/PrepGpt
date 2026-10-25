package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;

import java.time.LocalDateTime;

public class AdminQuestionResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;
    private String expectedAnswerGuidance;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminQuestionResponse() {}

    public AdminQuestionResponse(Long id, Long categoryId, String categoryName,
                                 String questionText, QuestionType questionType,
                                 Difficulty difficulty, String expectedAnswerGuidance,
                                 boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.expectedAnswerGuidance = expectedAnswerGuidance;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public String getExpectedAnswerGuidance() { return expectedAnswerGuidance; }
    public void setExpectedAnswerGuidance(String expectedAnswerGuidance) { this.expectedAnswerGuidance = expectedAnswerGuidance; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
