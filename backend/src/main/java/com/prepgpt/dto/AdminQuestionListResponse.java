package com.prepgpt.dto;

import java.util.List;

public class AdminQuestionListResponse {

    private List<AdminQuestionResponse> questions;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public AdminQuestionListResponse() {}

    public AdminQuestionListResponse(List<AdminQuestionResponse> questions, long totalElements,
                                     int totalPages, int page, int size) {
        this.questions = questions;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public List<AdminQuestionResponse> getQuestions() { return questions; }
    public void setQuestions(List<AdminQuestionResponse> questions) { this.questions = questions; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
