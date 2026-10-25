package com.prepgpt.dto;

import com.prepgpt.entity.AdaptiveDecision;
import com.prepgpt.entity.Difficulty;

public class AIFollowUpResponse {

    private Boolean shouldAskFollowUp;
    private String question;
    private String reason;
    private Difficulty difficulty;

    private AdaptiveDecision adaptiveDecision;
    private String targetConcept;
    private String userFacingMessage;
    private Difficulty nextDifficulty;
    private String expectedFocus;

    public AIFollowUpResponse() {
    }

    public AIFollowUpResponse(Boolean shouldAskFollowUp, String question, String reason, Difficulty difficulty) {
        this.shouldAskFollowUp = shouldAskFollowUp;
        this.question = question;
        this.reason = reason;
        this.difficulty = difficulty;
        this.adaptiveDecision = Boolean.TRUE.equals(shouldAskFollowUp) ? AdaptiveDecision.FOLLOW_UP : AdaptiveDecision.CHANGE_TOPIC;
        this.nextDifficulty = difficulty;
    }

    public AIFollowUpResponse(AdaptiveDecision adaptiveDecision, String reason, String targetConcept,
                              String userFacingMessage, Difficulty nextDifficulty, String nextQuestion,
                              String expectedFocus) {
        this.adaptiveDecision = adaptiveDecision;
        this.reason = reason;
        this.targetConcept = targetConcept;
        this.userFacingMessage = userFacingMessage;
        this.nextDifficulty = nextDifficulty;
        this.question = nextQuestion;
        this.expectedFocus = expectedFocus;
        this.shouldAskFollowUp = (adaptiveDecision == AdaptiveDecision.FOLLOW_UP || adaptiveDecision == AdaptiveDecision.DEEP_DIVE || adaptiveDecision == AdaptiveDecision.SIMPLIFY);
        this.difficulty = nextDifficulty;
    }

    public Boolean getShouldAskFollowUp() {
        return shouldAskFollowUp;
    }

    public void setShouldAskFollowUp(Boolean shouldAskFollowUp) {
        this.shouldAskFollowUp = shouldAskFollowUp;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getNextQuestion() {
        return question;
    }

    public void setNextQuestion(String nextQuestion) {
        this.question = nextQuestion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public AdaptiveDecision getAdaptiveDecision() {
        return adaptiveDecision;
    }

    public void setAdaptiveDecision(AdaptiveDecision adaptiveDecision) {
        this.adaptiveDecision = adaptiveDecision;
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

    public String getExpectedFocus() {
        return expectedFocus;
    }

    public void setExpectedFocus(String expectedFocus) {
        this.expectedFocus = expectedFocus;
    }
}
