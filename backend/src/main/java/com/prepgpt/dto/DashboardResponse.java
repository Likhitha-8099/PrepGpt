package com.prepgpt.dto;

import java.util.List;

public class DashboardResponse {

    private UserSummaryResponse user;
    private StudentProfileResponse profile;
    private DashboardStatisticsResponse statistics;
    private List<InterviewSessionResponse> recentInterviews;

    public DashboardResponse() {
    }

    public DashboardResponse(UserSummaryResponse user, StudentProfileResponse profile,
                             DashboardStatisticsResponse statistics, List<InterviewSessionResponse> recentInterviews) {
        this.user = user;
        this.profile = profile;
        this.statistics = statistics;
        this.recentInterviews = recentInterviews;
    }

    public UserSummaryResponse getUser() {
        return user;
    }

    public void setUser(UserSummaryResponse user) {
        this.user = user;
    }

    public StudentProfileResponse getProfile() {
        return profile;
    }

    public void setProfile(StudentProfileResponse profile) {
        this.profile = profile;
    }

    public DashboardStatisticsResponse getStatistics() {
        return statistics;
    }

    public void setStatistics(DashboardStatisticsResponse statistics) {
        this.statistics = statistics;
    }

    public List<InterviewSessionResponse> getRecentInterviews() {
        return recentInterviews;
    }

    public void setRecentInterviews(List<InterviewSessionResponse> recentInterviews) {
        this.recentInterviews = recentInterviews;
    }
}
