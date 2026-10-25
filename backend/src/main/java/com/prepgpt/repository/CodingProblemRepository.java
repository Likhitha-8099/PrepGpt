package com.prepgpt.repository;

import com.prepgpt.entity.CodingProblem;
import com.prepgpt.entity.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface CodingProblemRepository extends JpaRepository<CodingProblem, Long> {
    Optional<CodingProblem> findByTitle(String title);
    List<CodingProblem> findByDifficulty(Difficulty difficulty);
    List<CodingProblem> findByDsaCategory(String dsaCategory);
    List<CodingProblem> findByDifficultyAndDsaCategory(Difficulty difficulty, String dsaCategory);
}
