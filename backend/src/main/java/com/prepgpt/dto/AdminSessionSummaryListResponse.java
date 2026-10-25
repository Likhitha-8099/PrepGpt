package com.prepgpt.dto;

import java.util.List;

public class AdminSessionSummaryListResponse {

    private List<AdminSessionSummaryResponse> sessions;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public AdminSessionSummaryListResponse() {}

    public AdminSessionSummaryListResponse(List<AdminSessionSummaryResponse> sessions,
                                           long totalElements, int totalPages, int page, int size) {
        this.sessions = sessions;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public List<AdminSessionSummaryResponse> getSessions() { return sessions; }
    public void setSessions(List<AdminSessionSummaryResponse> sessions) { this.sessions = sessions; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
