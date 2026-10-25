package com.prepgpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_token_usage", indexes = {
        @Index(name = "idx_token_usage_created_at", columnList = "created_at"),
        @Index(name = "idx_token_usage_operation", columnList = "operation_type")
})
public class AiTokenUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt_tokens", nullable = true)
    private Integer promptTokens;

    @Column(name = "completion_tokens", nullable = true)
    private Integer completionTokens;

    @Column(name = "total_tokens", nullable = true)
    private Integer totalTokens;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider = "GROQ";

    @Column(name = "model", nullable = true, length = 64)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 32)
    private AiOperationType operationType = AiOperationType.GENERAL_AI;

    @Column(name = "session_id", nullable = true)
    private Long sessionId;

    @Column(name = "response_id", nullable = true)
    private Long responseId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AiTokenUsage() {}

    public AiTokenUsage(AiOperationType operationType, String provider, String model,
                        Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        this.operationType = operationType;
        this.provider = provider;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public AiOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(AiOperationType operationType) {
        this.operationType = operationType;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getResponseId() {
        return responseId;
    }

    public void setResponseId(Long responseId) {
        this.responseId = responseId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
