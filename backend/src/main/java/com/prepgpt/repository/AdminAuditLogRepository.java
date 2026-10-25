package com.prepgpt.repository;

import com.prepgpt.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AdminAuditLogRepository — ONLY injected into AdminService.
 * No controller accesses this repository directly.
 * No delete or update methods are exposed.
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);
    List<AdminAuditLog> findByAdminEmailOrderByPerformedAtDesc(String adminEmail);
}
