package com.prepgpt.dto;

import java.util.List;

public class AdminAuditLogListResponse {

    private List<AdminAuditLogResponse> entries;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public AdminAuditLogListResponse() {}

    public AdminAuditLogListResponse(List<AdminAuditLogResponse> entries, long totalElements,
                                     int totalPages, int page, int size) {
        this.entries = entries;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public List<AdminAuditLogResponse> getEntries() { return entries; }
    public void setEntries(List<AdminAuditLogResponse> entries) { this.entries = entries; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
