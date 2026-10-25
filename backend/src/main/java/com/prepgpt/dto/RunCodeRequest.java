package com.prepgpt.dto;

import jakarta.validation.constraints.NotBlank;

public class RunCodeRequest {

    @NotBlank(message = "Language is required")
    private String language; // "JAVA" or "PYTHON"

    @NotBlank(message = "Code cannot be empty")
    private String code;

    public RunCodeRequest() {
    }

    public RunCodeRequest(String language, String code) {
        this.language = language;
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
