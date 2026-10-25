package com.prepgpt.repository;

import com.prepgpt.entity.InterviewSession;
import com.prepgpt.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<InterviewSession> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);
    List<InterviewSession> findByJobDescriptionId(Long jobDescriptionId);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, SessionStatus status);

    // Admin queries
    long countByStatus(SessionStatus status);
    Page<InterviewSession> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<InterviewSession> findByStatusOrderByCreatedAtDesc(SessionStatus status, Pageable pageable);
    Page<InterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SessionStatus status, Pageable pageable);
}

