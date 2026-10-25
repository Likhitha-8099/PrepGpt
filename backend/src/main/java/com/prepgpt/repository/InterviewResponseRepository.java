package com.prepgpt.repository;

import com.prepgpt.entity.InterviewResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewResponseRepository extends JpaRepository<InterviewResponse, Long> {
    Optional<InterviewResponse> findByInterviewQuestionId(Long interviewQuestionId);
    boolean existsByInterviewQuestionId(Long interviewQuestionId);
    long countByInterviewQuestionInterviewSessionId(Long sessionId);
}
