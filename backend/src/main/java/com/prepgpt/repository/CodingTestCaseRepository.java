package com.prepgpt.repository;

import com.prepgpt.entity.CodingTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodingTestCaseRepository extends JpaRepository<CodingTestCase, Long> {
    List<CodingTestCase> findByCodingProblemIdOrderByDisplayOrderAsc(Long codingProblemId);
    List<CodingTestCase> findByCodingProblemIdAndIsHiddenFalseOrderByDisplayOrderAsc(Long codingProblemId);
}
