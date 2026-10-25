package com.prepgpt.repository;

import com.prepgpt.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    // Admin queries
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByIsActiveTrue();
    long countByRoleNameAndIsActiveTrue(String roleName);

    /**
     * Pessimistic write-locking query to inspect active administrators concurrently.
     * Bypasses stale MVCC snapshots and guarantees atomic evaluation of the
     * last-active-admin invariant under concurrent requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.isActive = true")
    List<User> findActiveAdminsForUpdate(@Param("roleName") String roleName);
}
