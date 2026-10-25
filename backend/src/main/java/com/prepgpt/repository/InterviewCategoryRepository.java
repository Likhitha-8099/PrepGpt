package com.prepgpt.repository;

import com.prepgpt.entity.InterviewCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewCategoryRepository extends JpaRepository<InterviewCategory, Long> {
    List<InterviewCategory> findByActiveTrue();
    Optional<InterviewCategory> findByName(String name);
}
