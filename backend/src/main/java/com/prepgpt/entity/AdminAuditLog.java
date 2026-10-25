package com.prepgpt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_audit_log_admin_email", columnList = "admin_email"),
        @Index(name = "idx_audit_log_performed_at", columnList = "performed_at")
})
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "target_entity_type", nullable = false, length = 30)
    private String targetEntityType;

    @Column(name = "target_entity_id", nullable = false)
    private Long targetEntityId;

    /**
     * JSON or plain-text extra context, e.g. {"oldRole":"ROLE_STUDENT","newRole":"ROLE_ADMIN","targetEmail":"..."}
     * Never contains passwords or secrets.
     */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    public AdminAuditLog() {
    }

    public AdminAuditLog(String adminEmail, String action,
                         String targetEntityType, Long targetEntityId, String detail) {
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
        this.detail = detail;
    }

    @PrePersist
    protected void onCreate() {
        this.performedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetEntityType() { return targetEntityType; }
    public void setTargetEntityType(String targetEntityType) { this.targetEntityType = targetEntityType; }

    public Long getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(Long targetEntityId) { this.targetEntityId = targetEntityId; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}
