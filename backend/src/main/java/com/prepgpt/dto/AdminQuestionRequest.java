package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminQuestionRequest {

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotBlank(message = "questionText must not be blank")
    @Size(max = 5000, message = "questionText must not exceed 5000 characters")
    private String questionText;

    @NotNull(message = "questionType is required")
    private QuestionType questionType;

    @NotNull(message = "difficulty is required")
    private Difficulty difficulty;

    private String expectedAnswerGuidance;

    public AdminQuestionRequest() {}

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public String getExpectedAnswerGuidance() { return expectedAnswerGuidance; }
    public void setExpectedAnswerGuidance(String expectedAnswerGuidance) { this.expectedAnswerGuidance = expectedAnswerGuidance; }
}
