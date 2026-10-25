package com.prepgpt.repository;

import com.prepgpt.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findByInterviewSessionIdOrderByQuestionOrderAsc(Long sessionId);
    Optional<InterviewQuestion> findByInterviewSessionIdAndQuestionOrder(Long sessionId, Integer questionOrder);
    long countByInterviewSessionId(Long sessionId);
    boolean existsByInterviewSessionId(Long sessionId);
}
