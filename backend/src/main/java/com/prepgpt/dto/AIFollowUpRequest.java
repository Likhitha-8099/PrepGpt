package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;

public class AIFollowUpRequest {

    private String categoryName;
    private Difficulty difficulty;
    private String previousQuestion;
    private String candidateAnswer;
    private AIEvaluationResponse evaluation;
    private String conversationContext;

    public AIFollowUpRequest() {
    }

    public AIFollowUpRequest(String categoryName, Difficulty difficulty, String previousQuestion,
                             String candidateAnswer, AIEvaluationResponse evaluation, String conversationContext) {
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.previousQuestion = previousQuestion;
        this.candidateAnswer = candidateAnswer;
        this.evaluation = evaluation;
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

    public String getPreviousQuestion() {
        return previousQuestion;
    }

    public void setPreviousQuestion(String previousQuestion) {
        this.previousQuestion = previousQuestion;
    }

    public String getCandidateAnswer() {
        return candidateAnswer;
    }

    public void setCandidateAnswer(String candidateAnswer) {
        this.candidateAnswer = candidateAnswer;
    }

    public AIEvaluationResponse getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(AIEvaluationResponse evaluation) {
        this.evaluation = evaluation;
    }

    public String getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(String conversationContext) {
        this.conversationContext = conversationContext;
    }
}
