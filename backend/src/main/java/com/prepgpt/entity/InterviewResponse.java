package com.prepgpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_responses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_response_interview_question", columnNames = {"interview_question_id"})
}, indexes = {
        @Index(name = "idx_response_interview_question", columnList = "interview_question_id")
})
public class InterviewResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "interview_question_id", nullable = false, unique = true)
    private InterviewQuestion interviewQuestion;

    @Column(name = "answer_text", columnDefinition = "TEXT", nullable = false)
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_method", nullable = false, length = 20)
    private InputMethod inputMethod = InputMethod.TEXT;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private LocalDateTime answeredAt;

    public InterviewResponse() {
    }

    public InterviewResponse(InterviewQuestion interviewQuestion, String answerText) {
        this(interviewQuestion, answerText, InputMethod.TEXT);
    }

    public InterviewResponse(InterviewQuestion interviewQuestion, String answerText, InputMethod inputMethod) {
        this.interviewQuestion = interviewQuestion;
        this.answerText = answerText;
        this.inputMethod = inputMethod != null ? inputMethod : InputMethod.TEXT;
    }

    @PrePersist
    protected void onCreate() {
        this.answeredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewQuestion getInterviewQuestion() {
        return interviewQuestion;
    }

    public void setInterviewQuestion(InterviewQuestion interviewQuestion) {
        this.interviewQuestion = interviewQuestion;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public InputMethod getInputMethod() {
        return inputMethod;
    }

    public void setInputMethod(InputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}
