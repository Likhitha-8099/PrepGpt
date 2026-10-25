package com.prepgpt.dto;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.InterviewMode;

import java.time.LocalDateTime;

public class ScoreTrendDto {

    private Long sessionId;
    private LocalDateTime date;
    private String categoryName;
    private Difficulty difficulty;
    private InterviewMode mode;
    private Double score;

    public ScoreTrendDto() {
    }

    public ScoreTrendDto(Long sessionId, LocalDateTime date, String categoryName,
                         Difficulty difficulty, InterviewMode mode, Double score) {
        this.sessionId = sessionId;
        this.date = date;
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.mode = mode;
        this.score = score;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
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

    public InterviewMode getMode() {
        return mode;
    }

    public void setMode(InterviewMode mode) {
        this.mode = mode;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
