package com.prepgpt.dto;

public class HealthCheckResponse {

    private String status;
    private String application;

    public HealthCheckResponse() {
    }

    public HealthCheckResponse(String status, String application) {
        this.status = status;
        this.application = application;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}
