package com.prepgpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_question_order", columnNames = {"interview_session_id", "question_order"})
}, indexes = {
        @Index(name = "idx_interview_question_session", columnList = "interview_session_id")
})
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "interview_session_id", nullable = false)
    private InterviewSession interviewSession;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "grounded_skill")
    private String groundedSkill;

    @Column(name = "grounded_responsibility")
    private String groundedResponsibility;

    @Column(name = "grounding_category", length = 50)
    private String groundingCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InterviewQuestion() {
    }

    public InterviewQuestion(InterviewSession interviewSession, Question question, Integer questionOrder) {
        this.interviewSession = interviewSession;
        this.question = question;
        this.questionOrder = questionOrder;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewSession getInterviewSession() {
        return interviewSession;
    }

    public void setInterviewSession(InterviewSession interviewSession) {
        this.interviewSession = interviewSession;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getGroundedSkill() {
        return groundedSkill;
    }

    public void setGroundedSkill(String groundedSkill) {
        this.groundedSkill = groundedSkill;
    }

    public String getGroundedResponsibility() {
        return groundedResponsibility;
    }

    public void setGroundedResponsibility(String groundedResponsibility) {
        this.groundedResponsibility = groundedResponsibility;
    }

    public String getGroundingCategory() {
        return groundingCategory;
    }

    public void setGroundingCategory(String groundingCategory) {
        this.groundingCategory = groundingCategory;
    }
}
