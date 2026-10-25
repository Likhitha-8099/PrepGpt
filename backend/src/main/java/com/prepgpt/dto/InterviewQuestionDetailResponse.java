package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.QuestionType;

import java.time.LocalDateTime;

public class InterviewQuestionDetailResponse {

    private Long id;
    private Integer questionOrder;
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;
    private String answerText;
    private LocalDateTime answeredAt;
    private EvaluationResponse evaluation;

    public InterviewQuestionDetailResponse() {
    }

    public InterviewQuestionDetailResponse(Long id, Integer questionOrder, String questionText,
                                          QuestionType questionType, Difficulty difficulty,
                                          String answerText, LocalDateTime answeredAt) {
        this(id, questionOrder, questionText, questionType, difficulty, answerText, answeredAt, null);
    }

    public InterviewQuestionDetailResponse(Long id, Integer questionOrder, String questionText,
                                          QuestionType questionType, Difficulty difficulty,
                                          String answerText, LocalDateTime answeredAt, EvaluationResponse evaluation) {
        this.id = id;
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.answerText = answerText;
        this.answeredAt = answeredAt;
        this.evaluation = evaluation;
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

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }

    public EvaluationResponse getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(EvaluationResponse evaluation) {
        this.evaluation = evaluation;
    }
}
