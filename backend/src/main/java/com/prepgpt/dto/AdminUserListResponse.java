package com.prepgpt.dto;

import java.util.List;

public class AdminUserListResponse {

    private List<AdminUserResponse> users;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public AdminUserListResponse() {}

    public AdminUserListResponse(List<AdminUserResponse> users, long totalElements,
                                 int totalPages, int page, int size) {
        this.users = users;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public List<AdminUserResponse> getUsers() { return users; }
    public void setUsers(List<AdminUserResponse> users) { this.users = users; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
