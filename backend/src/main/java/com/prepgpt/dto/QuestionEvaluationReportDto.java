package com.prepgpt.dto;

import com.prepgpt.entity.AdaptiveDecision;
import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.InputMethod;
import com.prepgpt.entity.QuestionType;

import java.util.List;

public class QuestionEvaluationReportDto {

    private Integer questionOrder;
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;

    private String candidateAnswer;
    private InputMethod inputMethod;

    private Integer score;
    private Integer correctness;
    private Integer relevance;
    private Integer clarity;
    private Integer technicalAccuracy;

    private List<String> strengths;
    private List<String> weaknesses;
    private String feedback;
    private String improvementSuggestion;

    private AdaptiveDecision adaptiveDecision;
    private String userFacingMessage;
    private String targetConcept;
    private Difficulty nextDifficulty;

    public QuestionEvaluationReportDto() {
    }

    public QuestionEvaluationReportDto(Integer questionOrder, String questionText, QuestionType questionType,
                                       Difficulty difficulty, String candidateAnswer, InputMethod inputMethod,
                                       Integer score, Integer correctness, Integer relevance, Integer clarity,
                                       Integer technicalAccuracy, List<String> strengths, List<String> weaknesses,
                                       String feedback, String improvementSuggestion, AdaptiveDecision adaptiveDecision,
                                       String userFacingMessage, String targetConcept, Difficulty nextDifficulty) {
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.candidateAnswer = candidateAnswer;
        this.inputMethod = inputMethod;
        this.score = score;
        this.correctness = correctness;
        this.relevance = relevance;
        this.clarity = clarity;
        this.technicalAccuracy = technicalAccuracy;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.feedback = feedback;
        this.improvementSuggestion = improvementSuggestion;
        this.adaptiveDecision = adaptiveDecision;
        this.userFacingMessage = userFacingMessage;
        this.targetConcept = targetConcept;
        this.nextDifficulty = nextDifficulty;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
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

    public String getCandidateAnswer() {
        return candidateAnswer;
    }

    public void setCandidateAnswer(String candidateAnswer) {
        this.candidateAnswer = candidateAnswer;
    }

    public InputMethod getInputMethod() {
        return inputMethod;
    }

    public void setInputMethod(InputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getCorrectness() {
        return correctness;
    }

    public void setCorrectness(Integer correctness) {
        this.correctness = correctness;
    }

    public Integer getRelevance() {
        return relevance;
    }

    public void setRelevance(Integer relevance) {
        this.relevance = relevance;
    }

    public Integer getClarity() {
        return clarity;
    }

    public void setClarity(Integer clarity) {
        this.clarity = clarity;
    }

    public Integer getTechnicalAccuracy() {
        return technicalAccuracy;
    }

    public void setTechnicalAccuracy(Integer technicalAccuracy) {
        this.technicalAccuracy = technicalAccuracy;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getImprovementSuggestion() {
        return improvementSuggestion;
    }

    public void setImprovementSuggestion(String improvementSuggestion) {
        this.improvementSuggestion = improvementSuggestion;
    }

    public AdaptiveDecision getAdaptiveDecision() {
        return adaptiveDecision;
    }

    public void setAdaptiveDecision(AdaptiveDecision adaptiveDecision) {
        this.adaptiveDecision = adaptiveDecision;
    }

    public String getUserFacingMessage() {
        return userFacingMessage;
    }

    public void setUserFacingMessage(String userFacingMessage) {
        this.userFacingMessage = userFacingMessage;
    }

    public String getTargetConcept() {
        return targetConcept;
    }

    public void setTargetConcept(String targetConcept) {
        this.targetConcept = targetConcept;
    }

    public Difficulty getNextDifficulty() {
        return nextDifficulty;
    }

    public void setNextDifficulty(Difficulty nextDifficulty) {
        this.nextDifficulty = nextDifficulty;
    }
}
