package com.prepgpt.dto;

public class AnswerResponse {

    private Boolean success;
    private Long questionId;
    private Boolean answered;
    private Boolean completed;
    private EvaluationResponse evaluation;

    public AnswerResponse() {
    }

    public AnswerResponse(Boolean success, Long questionId, Boolean answered, Boolean completed) {
        this.success = success;
        this.questionId = questionId;
        this.answered = answered;
        this.completed = completed;
    }

    public AnswerResponse(Boolean success, Long questionId, Boolean answered, Boolean completed, EvaluationResponse evaluation) {
        this.success = success;
        this.questionId = questionId;
        this.answered = answered;
        this.completed = completed;
        this.evaluation = evaluation;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public EvaluationResponse getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(EvaluationResponse evaluation) {
        this.evaluation = evaluation;
    }
}
