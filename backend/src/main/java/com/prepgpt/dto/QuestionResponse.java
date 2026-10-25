package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;

public class QuestionResponse {

    private Long id; // InterviewQuestion id
    private Integer questionOrder;
    private Integer totalQuestions;
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;
    private Boolean answered;
    private Integer progress;

    public QuestionResponse() {
    }

    public QuestionResponse(Long id, Integer questionOrder, Integer totalQuestions, String questionText,
                            QuestionType questionType, Difficulty difficulty, Boolean answered, Integer progress) {
        this.id = id;
        this.questionOrder = questionOrder;
        this.totalQuestions = totalQuestions;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.answered = answered;
        this.progress = progress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}
