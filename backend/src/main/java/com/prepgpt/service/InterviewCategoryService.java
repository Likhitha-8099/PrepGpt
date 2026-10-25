package com.prepgpt.service;

import com.prepgpt.dto.InterviewCategoryResponse;
import com.prepgpt.entity.InterviewCategory;
import com.prepgpt.repository.InterviewCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InterviewCategoryService {

    private final InterviewCategoryRepository categoryRepository;

    public InterviewCategoryService(InterviewCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InterviewCategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InterviewCategoryResponse mapToResponse(InterviewCategory category) {
        return new InterviewCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }
}
