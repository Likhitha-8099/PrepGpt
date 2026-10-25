package com.prepgpt.repository;

import com.prepgpt.entity.Difficulty;
import com.prepgpt.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCategoryIdAndDifficultyAndActiveTrue(Long categoryId, Difficulty difficulty);
    List<Question> findByCategoryIdAndActiveTrue(Long categoryId);
    long countByCategoryIdAndDifficultyAndActiveTrue(Long categoryId, Difficulty difficulty);
    long countByCategoryIdAndActiveTrue(Long categoryId);
    boolean existsByCategoryIdAndQuestionText(Long categoryId, String questionText);

    // Admin queries — return all questions including inactive ones
    long countByActiveTrue();
    Page<Question> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Question> findByCategoryIdOrderByCreatedAtDesc(Long categoryId, Pageable pageable);
    Page<Question> findByDifficultyOrderByCreatedAtDesc(Difficulty difficulty, Pageable pageable);
    Page<Question> findByCategoryIdAndDifficultyOrderByCreatedAtDesc(Long categoryId, Difficulty difficulty, Pageable pageable);
}

