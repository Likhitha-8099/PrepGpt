package com.prepgpt.repository;

import com.prepgpt.entity.InterviewEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewEvaluationRepository extends JpaRepository<InterviewEvaluation, Long> {
    Optional<InterviewEvaluation> findByInterviewResponseId(Long responseId);
    Optional<InterviewEvaluation> findByInterviewResponseInterviewQuestionId(Long interviewQuestionId);
    boolean existsByInterviewResponseInterviewQuestionId(Long interviewQuestionId);

    @Query("SELECT AVG(e.score) FROM InterviewEvaluation e WHERE e.score IS NOT NULL")
    Optional<Double> findAveragePlatformScore();

    @Query("SELECT AVG(e.score) FROM InterviewEvaluation e " +
           "WHERE e.score IS NOT NULL " +
           "AND e.interviewResponse.interviewQuestion.interviewSession.id = :sessionId")
    Optional<Double> findAverageScoreBySessionId(Long sessionId);

    java.util.List<InterviewEvaluation> findByStatusAndNextRetryAtLessThanEqualAndRetryCountLessThanOrderByNextRetryAtAsc(
            com.prepgpt.entity.EvaluationStatus status,
            java.time.LocalDateTime dateTime,
            Integer maxAttempts
    );
}
