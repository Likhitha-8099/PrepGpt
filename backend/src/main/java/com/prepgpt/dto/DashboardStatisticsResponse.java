package com.prepgpt.dto;

public class DashboardStatisticsResponse {

    private long totalInterviews;
    private long completedInterviews;
    private long inProgressInterviews;
    private long configuredInterviews;

    public DashboardStatisticsResponse() {
    }

    public DashboardStatisticsResponse(long totalInterviews, long completedInterviews, long inProgressInterviews, long configuredInterviews) {
        this.totalInterviews = totalInterviews;
        this.completedInterviews = completedInterviews;
        this.inProgressInterviews = inProgressInterviews;
        this.configuredInterviews = configuredInterviews;
    }

    public long getTotalInterviews() {
        return totalInterviews;
    }

    public void setTotalInterviews(long totalInterviews) {
        this.totalInterviews = totalInterviews;
    }

    public long getCompletedInterviews() {
        return completedInterviews;
    }

    public void setCompletedInterviews(long completedInterviews) {
        this.completedInterviews = completedInterviews;
    }

    public long getInProgressInterviews() {
        return inProgressInterviews;
    }

    public void setInProgressInterviews(long inProgressInterviews) {
        this.inProgressInterviews = inProgressInterviews;
    }

    public long getConfiguredInterviews() {
        return configuredInterviews;
    }

    public void setConfiguredInterviews(long configuredInterviews) {
        this.configuredInterviews = configuredInterviews;
    }
}
