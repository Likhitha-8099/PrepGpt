package com.prepgpt.dto;

import java.util.List;

public class AnalyticsOverviewResponse {

    private Long totalInterviews;
    private Long completedInterviews;
    private Long inProgressInterviews;
    private Double completionRatePercentage;

    private Double averageOverallScore;
    private Double bestOverallScore;
    private Double latestOverallScore;

    private Long totalQuestionsAnswered;

    private List<ScoreTrendDto> scoreTrend;
    private List<String> strongestCategories;
    private List<String> weakestCategories;

    public AnalyticsOverviewResponse() {
    }

    public AnalyticsOverviewResponse(Long totalInterviews, Long completedInterviews, Long inProgressInterviews,
                                     Double completionRatePercentage, Double averageOverallScore,
                                     Double bestOverallScore, Double latestOverallScore, Long totalQuestionsAnswered,
                                     List<ScoreTrendDto> scoreTrend, List<String> strongestCategories,
                                     List<String> weakestCategories) {
        this.totalInterviews = totalInterviews;
        this.completedInterviews = completedInterviews;
        this.inProgressInterviews = inProgressInterviews;
        this.completionRatePercentage = completionRatePercentage;
        this.averageOverallScore = averageOverallScore;
        this.bestOverallScore = bestOverallScore;
        this.latestOverallScore = latestOverallScore;
        this.totalQuestionsAnswered = totalQuestionsAnswered;
        this.scoreTrend = scoreTrend;
        this.strongestCategories = strongestCategories;
        this.weakestCategories = weakestCategories;
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

    public Long getInProgressInterviews() {
        return inProgressInterviews;
    }

    public void setInProgressInterviews(Long inProgressInterviews) {
        this.inProgressInterviews = inProgressInterviews;
    }

    public Double getCompletionRatePercentage() {
        return completionRatePercentage;
    }

    public void setCompletionRatePercentage(Double completionRatePercentage) {
        this.completionRatePercentage = completionRatePercentage;
    }

    public Double getAverageOverallScore() {
        return averageOverallScore;
    }

    public void setAverageOverallScore(Double averageOverallScore) {
        this.averageOverallScore = averageOverallScore;
    }

    public Double getBestOverallScore() {
        return bestOverallScore;
    }

    public void setBestOverallScore(Double bestOverallScore) {
        this.bestOverallScore = bestOverallScore;
    }

    public Double getLatestOverallScore() {
        return latestOverallScore;
    }

    public void setLatestOverallScore(Double latestOverallScore) {
        this.latestOverallScore = latestOverallScore;
    }

    public Long getTotalQuestionsAnswered() {
        return totalQuestionsAnswered;
    }

    public void setTotalQuestionsAnswered(Long totalQuestionsAnswered) {
        this.totalQuestionsAnswered = totalQuestionsAnswered;
    }

    public List<ScoreTrendDto> getScoreTrend() {
        return scoreTrend;
    }

    public void setScoreTrend(List<ScoreTrendDto> scoreTrend) {
        this.scoreTrend = scoreTrend;
    }

    public List<String> getStrongestCategories() {
        return strongestCategories;
    }

    public void setStrongestCategories(List<String> strongestCategories) {
        this.strongestCategories = strongestCategories;
    }

    public List<String> getWeakestCategories() {
        return weakestCategories;
    }

    public void setWeakestCategories(List<String> weakestCategories) {
        this.weakestCategories = weakestCategories;
    }
}
