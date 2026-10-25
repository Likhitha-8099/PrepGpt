package com.prepgpt.dto;

import java.time.LocalDateTime;

public class AdminAuditLogResponse {

    private Long id;
    private String adminEmail;
    private String action;
    private String targetEntityType;
    private Long targetEntityId;
    private String detail;
    private LocalDateTime performedAt;

    public AdminAuditLogResponse() {}

    public AdminAuditLogResponse(Long id, String adminEmail, String action,
                                 String targetEntityType, Long targetEntityId,
                                 String detail, LocalDateTime performedAt) {
        this.id = id;
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
        this.detail = detail;
        this.performedAt = performedAt;
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
