package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;

public class AIEvaluationRequest {

    private String categoryName;
    private Difficulty difficulty;
    private String question;
    private String candidateAnswer;
    private String expectedAnswerGuidance;
    private String conversationContext;

    public AIEvaluationRequest() {
    }

    public AIEvaluationRequest(String categoryName, Difficulty difficulty, String question,
                               String candidateAnswer, String expectedAnswerGuidance, String conversationContext) {
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.question = question;
        this.candidateAnswer = candidateAnswer;
        this.expectedAnswerGuidance = expectedAnswerGuidance;
        this.conversationContext = conversationContext;
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCandidateAnswer() {
        return candidateAnswer;
    }

    public void setCandidateAnswer(String candidateAnswer) {
        this.candidateAnswer = candidateAnswer;
    }

    public String getExpectedAnswerGuidance() {
        return expectedAnswerGuidance;
    }

    public void setExpectedAnswerGuidance(String expectedAnswerGuidance) {
        this.expectedAnswerGuidance = expectedAnswerGuidance;
    }

    public String getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(String conversationContext) {
        this.conversationContext = conversationContext;
    }
}
