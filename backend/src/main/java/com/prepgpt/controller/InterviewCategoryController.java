package com.prepgpt.controller;

import com.prepgpt.dto.InterviewCategoryResponse;
import com.prepgpt.service.InterviewCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interview-categories")
@Tag(name = "Interview Category API", description = "Endpoints for fetching active interview tracks and categories")
public class InterviewCategoryController {

    private final InterviewCategoryService categoryService;

    public InterviewCategoryController(InterviewCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Get active interview categories", description = "Fetches all active interview practice tracks available for candidates")
    public ResponseEntity<List<InterviewCategoryResponse>> getActiveCategories() {
        List<InterviewCategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }
}
