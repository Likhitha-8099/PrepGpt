package com.prepgpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateJobDescriptionRequest {

    @NotBlank(message = "Job description text cannot be empty")
    @Size(max = 10000, message = "Job description exceeds maximum allowed length of 10,000 characters")
    private String rawText;

    private String targetRole;

    private String companyName;

    public CreateJobDescriptionRequest() {
    }

    public CreateJobDescriptionRequest(String rawText, String targetRole, String companyName) {
        this.rawText = rawText;
        this.targetRole = targetRole;
        this.companyName = companyName;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
