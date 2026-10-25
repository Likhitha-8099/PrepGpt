package com.prepgpt.repository;

import com.prepgpt.entity.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
    List<CodeSubmission> findByInterviewSessionId(Long interviewSessionId);
    List<CodeSubmission> findByInterviewQuestionIdOrderByCreatedAtDesc(Long interviewQuestionId);
}
