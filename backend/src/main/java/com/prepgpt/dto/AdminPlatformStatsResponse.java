package com.prepgpt.dto;

public class AdminPlatformStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long totalSessions;
    private long completedSessions;
    private long inProgressSessions;
    private long totalQuestions;
    private long activeQuestions;
    private long totalCodingProblems;
    private long totalJobDescriptions;
    private long totalResumes;
    private Double averagePlatformScore;

    public AdminPlatformStatsResponse() {}

    public AdminPlatformStatsResponse(long totalUsers, long activeUsers, long inactiveUsers,
                                      long totalSessions, long completedSessions, long inProgressSessions,
                                      long totalQuestions, long activeQuestions,
                                      long totalCodingProblems, long totalJobDescriptions,
                                      long totalResumes, Double averagePlatformScore) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.inactiveUsers = inactiveUsers;
        this.totalSessions = totalSessions;
        this.completedSessions = completedSessions;
        this.inProgressSessions = inProgressSessions;
        this.totalQuestions = totalQuestions;
        this.activeQuestions = activeQuestions;
        this.totalCodingProblems = totalCodingProblems;
        this.totalJobDescriptions = totalJobDescriptions;
        this.totalResumes = totalResumes;
        this.averagePlatformScore = averagePlatformScore;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getInactiveUsers() { return inactiveUsers; }
    public void setInactiveUsers(long inactiveUsers) { this.inactiveUsers = inactiveUsers; }

    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }

    public long getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(long completedSessions) { this.completedSessions = completedSessions; }

    public long getInProgressSessions() { return inProgressSessions; }
    public void setInProgressSessions(long inProgressSessions) { this.inProgressSessions = inProgressSessions; }

    public long getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(long totalQuestions) { this.totalQuestions = totalQuestions; }

    public long getActiveQuestions() { return activeQuestions; }
    public void setActiveQuestions(long activeQuestions) { this.activeQuestions = activeQuestions; }

    public long getTotalCodingProblems() { return totalCodingProblems; }
    public void setTotalCodingProblems(long totalCodingProblems) { this.totalCodingProblems = totalCodingProblems; }

    public long getTotalJobDescriptions() { return totalJobDescriptions; }
    public void setTotalJobDescriptions(long totalJobDescriptions) { this.totalJobDescriptions = totalJobDescriptions; }

    public long getTotalResumes() { return totalResumes; }
    public void setTotalResumes(long totalResumes) { this.totalResumes = totalResumes; }

    public Double getAveragePlatformScore() { return averagePlatformScore; }
    public void setAveragePlatformScore(Double averagePlatformScore) { this.averagePlatformScore = averagePlatformScore; }
}
