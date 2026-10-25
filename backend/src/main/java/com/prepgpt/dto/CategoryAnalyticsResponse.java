package com.prepgpt.dto;

public class CategoryAnalyticsResponse {

    private Long categoryId;
    private String categoryName;
    private Long totalInterviews;
    private Long completedInterviews;

    private Double averageScore;
    private Double bestScore;

    private Double averageCorrectness;
    private Double averageRelevance;
    private Double averageClarity;
    private Double averageTechnicalAccuracy;

    private String performanceLevel;

    public CategoryAnalyticsResponse() {
    }

    public CategoryAnalyticsResponse(Long categoryId, String categoryName, Long totalInterviews,
                                     Long completedInterviews, Double averageScore, Double bestScore,
                                     Double averageCorrectness, Double averageRelevance, Double averageClarity,
                                     Double averageTechnicalAccuracy, String performanceLevel) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalInterviews = totalInterviews;
        this.completedInterviews = completedInterviews;
        this.averageScore = averageScore;
        this.bestScore = bestScore;
        this.averageCorrectness = averageCorrectness;
        this.averageRelevance = averageRelevance;
        this.averageClarity = averageClarity;
        this.averageTechnicalAccuracy = averageTechnicalAccuracy;
        this.performanceLevel = performanceLevel;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getTotalInterviews() {
        return totalInterviews;
    }

    public void setTotalInterviews(Long totalInterviews) {
        this.totalInterviews = totalInterviews;
    }

    public Long getCompletedInterviews() {
        return completedInterviews;
    }

    public void setCompletedInterviews(Long completedInterviews) {
        this.completedInterviews = completedInterviews;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public Double getBestScore() {
        return bestScore;
    }

    public void setBestScore(Double bestScore) {
        this.bestScore = bestScore;
    }

    public Double getAverageCorrectness() {
        return averageCorrectness;
    }

    public void setAverageCorrectness(Double averageCorrectness) {
        this.averageCorrectness = averageCorrectness;
    }

    public Double getAverageRelevance() {
        return averageRelevance;
    }

    public void setAverageRelevance(Double averageRelevance) {
        this.averageRelevance = averageRelevance;
    }

    public Double getAverageClarity() {
        return averageClarity;
    }

    public void setAverageClarity(Double averageClarity) {
        this.averageClarity = averageClarity;
    }

    public Double getAverageTechnicalAccuracy() {
        return averageTechnicalAccuracy;
    }

    public void setAverageTechnicalAccuracy(Double averageTechnicalAccuracy) {
        this.averageTechnicalAccuracy = averageTechnicalAccuracy;
    }

    public String getPerformanceLevel() {
        return performanceLevel;
    }

    public void setPerformanceLevel(String performanceLevel) {
        this.performanceLevel = performanceLevel;
    }
}
