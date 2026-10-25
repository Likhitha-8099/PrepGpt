package com.prepgpt.dto;

import com.prepgpt.entity.SessionStatus;

public class InterviewStatusResponse {

    private Long sessionId;
    private SessionStatus status;
    private Integer currentQuestion;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer progress;

    public InterviewStatusResponse() {
    }

    public InterviewStatusResponse(Long sessionId, SessionStatus status, Integer currentQuestion,
                                   Integer totalQuestions, Integer answeredQuestions, Integer progress) {
        this.sessionId = sessionId;
        this.status = status;
        this.currentQuestion = currentQuestion;
        this.totalQuestions = totalQuestions;
        this.answeredQuestions = answeredQuestions;
        this.progress = progress;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Integer getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(Integer currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(Integer answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}
