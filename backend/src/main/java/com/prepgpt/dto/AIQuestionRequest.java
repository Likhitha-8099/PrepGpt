package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;

import java.util.List;

public class AIQuestionRequest {

    private String categoryName;
    private Difficulty difficulty;
    private QuestionType questionType;
    private List<String> previousQuestions;
    private String conversationContext;
    private Integer questionNumber;
    private Integer totalQuestions;

    public AIQuestionRequest() {
    }

    public AIQuestionRequest(String categoryName, Difficulty difficulty, QuestionType questionType,
                             List<String> previousQuestions, String conversationContext,
                             Integer questionNumber, Integer totalQuestions) {
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.previousQuestions = previousQuestions;
        this.conversationContext = conversationContext;
        this.questionNumber = questionNumber;
        this.totalQuestions = totalQuestions;
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

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public List<String> getPreviousQuestions() {
        return previousQuestions;
    }

    public void setPreviousQuestions(List<String> previousQuestions) {
        this.previousQuestions = previousQuestions;
    }

    public String getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(String conversationContext) {
        this.conversationContext = conversationContext;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
}
